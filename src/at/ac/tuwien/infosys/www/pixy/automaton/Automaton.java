/*
 * dk.brics.automaton
 *
 * Copyright (c) 2001-2006 Anders Moeller
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package at.ac.tuwien.infosys.www.pixy.automaton;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.*;

/**
 * Finite-state automaton with regular expression operations.
 * <p/>
 * Automatons are represented using {@link State} and {@link Transition} objects.
 * Implicitly, all states and transitions of an automaton are reachable from its initial state.
 * If the states or transitions are manipulated manually, the {@link #restoreInvariant()}
 * and {@link #setDeterministic(boolean)} methods should be used afterwards to restore
 * certain representation invariants that are assumed by the built-in automaton operations.
 *
 * Class invariants:
 *
 * - An automaton is either represented explicitly (with State and Transition objects)
 *   or with a singleton string in case the automaton accepts exactly one string.
 * - Automatons are always reduced (see reduce())
 *   and have no transitions to dead states (see removeDeadTransitions()).
 * - If an automaton is nondeterministic, then isDeterministic() returns false (but
 *   the converse is not required).
 *
 * @author Anders M&oslash;ller &lt;<a href="mailto:amoeller@brics.dk">amoeller@brics.dk</a>&gt;
 */
public class Automaton
    implements Serializable, Cloneable {

    static final long serialVersionUID = 10001;

    /**
     * Minimize using Huffman's O(n<sup>2</sup>) algorithm.
     * This is the standard text-book algorithm.
     *
     * @see #setMinimization(int)
     */
    public static final int MINIMIZE_HUFFMAN = 0;

    /**
     * Minimize using Brzozowski's O(2<sup>n</sup>) algorithm.
     * This algorithm uses the reverse-determinize-reverse-determinize trick, which has a bad
     * worst-case behavior but often works very well in practice
     * (even better than Hopcroft's!).
     *
     * @see #setMinimization(int)
     */
    public static final int MINIMIZE_BRZOZOWSKI = 1;

    /**
     * Minimize using Hopcroft's O(n log n) algorithm.
     * This is regarded as one of the most generally efficient algorithms that exist.
     *
     * @see #setMinimization(int)
     */
    public static final int MINIMIZE_HOPCROFT = 2;

    /**
     * Selects minimization algorithm (default: <code>MINIMIZE_HOPCROFT</code>).
     */
    static int minimization = MINIMIZE_HOPCROFT;

    /**
     * Initial state of this automaton.
     */
    State initial;

    /**
     * If true, then this automaton is definitely deterministic
     * (i.e., there are no choices for any run, but a run may crash).
     */
    boolean deterministic;

    /**
     * Extra data associated with this automaton.
     */
    Object info;

    /**
     * Hash code. Recomputed by {@link #minimize()}.
     */
    int hash_code;

    /**
     * Singleton string. Null if not applicable.
     */
    String singleton;

    /**
     * Minimize always flag.
     */
    static boolean minimize_always;

    /**
     * Constructs new automaton that accepts the empty language.
     * Using this constructor, automata can be constructed manually from
     * {@link State} and {@link Transition} objects.
     *
     * @see #setInitialState(State)
     * @see State
     * @see Transition
     */
    public Automaton() {
        initial = new State();
        deterministic = true;
    }

    // performs a simple data flow analysis that maps states to [open,closed]
    // (with respect to "apostrophe areas"); if there are indirectly tainted
    // transitions with a [closed] source state, it means that we have a
    // problem => returns true; else: false
    public boolean hasDangerousIndirectTaint() {

        // "data flow analysis information"
        Map<State, Integer> dfi = new HashMap<>();
        int open = 0;
        int closed = 1;
        dfi.put(this.initial, closed);

        // initialize worklist with start state
        LinkedList<State> worklist = new LinkedList<>();
        worklist.add(this.initial);

        while (worklist.size() > 0) {
            State source = worklist.removeFirst();

            for (Transition t : source.transitions) {
                int sourceInfo = dfi.get(source);
                // the value that is to be propagated
                int propagate = -1;
                // if the current transition does not correspond to a certain
                // character: simply propagate source info
                if (t.min != t.max) {
                    propagate = sourceInfo;
                }
                // if the current transition is labeled with one of these,
                // "flip" the info that is to be propagated
                if (t.min == '"' || t.min == '\'') {
                    if (sourceInfo == open) {
                        propagate = closed;
                    } else if (sourceInfo == closed) {
                        propagate = open;
                    } else {
                        throw new RuntimeException("SNH: " + sourceInfo);
                    }
                } else {
                    propagate = sourceInfo;
                }

                // propagate to target state
                State target = t.getDest();
                Integer targetInfo = dfi.get(target);
                if (targetInfo == null) {
                    // the target node has not been visited yet
                    dfi.put(target, propagate);
                    worklist.add(target);
                } else {
                    int newInfo = this.lub(propagate, targetInfo);
                    if (newInfo == targetInfo) {
                        // no change, don't continue here
                    } else {
                        // update analysis info and worklist
                        dfi.put(target, newInfo);
                        worklist.add(target);
                    }
                }
            }
        }

        for (Map.Entry<State, Integer> entry : dfi.entrySet()) {
            State s = entry.getKey();
            Integer info = entry.getValue();
            if (info == open) {
                // no danger here
                continue;
            }
            for (Transition t : s.getTransitions()) {
                if (t.taint == Transition.Taint.Indirectly) {
                    // detected indirect vulnerability!
                    return true;
                }
            }
        }
        return false;
    }

    // helper for hasDangerousIndirectTaint()
    private int lub(int a, int b) {
        if (a == 1 || b == 1) {
            return 1;
        } else {
            return 0;
        }
    }

    // returns true if this automaton contains at least one transition
    // that is directly tainted
    public boolean hasDirectlyTaintedTransitions() {
        for (State state : this.getStates()) {
            for (Transition trans : state.getTransitions()) {
                if (trans.taint == Transition.Taint.Directly) {
                    return true;
                }
            }
        }
        return false;
    }

    // returns true if this automaton contains at least one transition
    // that is indirectly tainted
    public boolean hasIndirectlyTaintedTransitions() {
        for (State state : this.getStates()) {
            for (Transition trans : state.getTransitions()) {
                if (trans.taint == Transition.Taint.Indirectly) {
                    return true;
                }
            }
        }
        return false;
    }

    // returns true if this automaton contains at least one transition
    // that is tainted in any way
    public boolean hasTaintedTransitions() {
        for (State state : this.getStates()) {
            for (Transition trans : state.getTransitions()) {
                if (trans.taint == Transition.Taint.Indirectly ||
                    trans.taint == Transition.Taint.Directly) {
                    return true;
                }
            }
        }
        return false;
    }

    // returns a map for incoming transitions: to -> fromSet;
    // states: the states of the automaton
    private Map<State, Set<State>> getReverseTransitions(Set<State> states) {
        Map<State, Set<State>> reverse = new HashMap<>();
        for (State r : states) {
            reverse.put(r, new HashSet<State>());
        }
        for (State r : states) {
            for (Transition t : r.getTransitions()) {
                reverse.get(t.to).add(r);
            }
        }
        return reverse;
    }

    void checkMinimizeAlways() {
        if (minimize_always)
            minimize();
    }

    public boolean isSingleton() {
        return singleton != null;
    }

    /**
     * gets initial state.
     *
     * @return state
     */
    public State getInitialState() {
        expandSingleton();
        return initial;
    }

    /**
     * Returns the set states that are reachable from the initial state.
     *
     * @return set of {@link State} objects
     */
    public Set<State> getStates() {
        expandSingleton();
        HashSet<State> visited = new HashSet<>();
        LinkedList<State> worklist = new LinkedList<>();
        worklist.add(initial);
        visited.add(initial);
        while (worklist.size() > 0) {
            State s = worklist.removeFirst();
            for (Transition t : s.transitions)
                if (!visited.contains(t.to)) {
                    visited.add(t.to);
                    worklist.add(t.to);
                }
        }
        return visited;
    }

    /**
     * Returns the set of reachable accept states.
     *
     * @return set of {@link State} objects
     */
    public Set<State> getAcceptStates() {
        expandSingleton();
        HashSet<State> accepts = new HashSet<>();
        HashSet<State> visited = new HashSet<>();
        LinkedList<State> worklist = new LinkedList<>();
        worklist.add(initial);
        visited.add(initial);
        while (worklist.size() > 0) {
            State s = worklist.removeFirst();
            if (s.accept)
                accepts.add(s);
            for (Transition t : s.transitions)
                if (!visited.contains(t.to)) {
                    visited.add(t.to);
                    worklist.add(t.to);
                }
        }
        return accepts;
    }

    /**
     * Assigns consecutive numbers to the given states.
     */
    static void setStateNumbers(Set<State> states) {
        int number = 0;
        for (State s : states)
            s.number = number++;
    }

    void setStateNumbersUnique() {
        expandSingleton();

        // queue for nodes that still have to be visited
        LinkedList<State> queue = new LinkedList<>();

        Set<State> visited = new HashSet<>();

        initial.number = 0;
        queue.add(initial);
        visited.add(initial);

        Comparator<Transition> comp = new TransComparator<>();
        this.bfIteratorHelper(queue, visited, comp);
    }

    private void bfIteratorHelper(
        LinkedList<State> queue, Set<State> visited,
        Comparator<Transition> comp) {

        State node = queue.removeFirst();

        // handle successors
        List<Transition> successors = new LinkedList<>(node.transitions);
        Collections.sort(successors, comp);

        for (Transition succ : successors) {
            // for all successors that have not been visited yet...
            if (!visited.contains(succ.to)) {
                succ.to.number = visited.size();
                // add it to the queue
                queue.add(succ.to);
                // mark it as visited
                visited.add(succ.to);
            }
        }

        // if the queue is non-empty: recurse
        if (queue.size() > 0) {
            bfIteratorHelper(queue, visited, comp);
        }
    }

    private class TransComparator<T>
        implements Comparator<T> {

        public int compare(T o1, T o2) {
            if (!(o1 instanceof Transition) || !(o2 instanceof Transition)) {
                throw new RuntimeException("SNH");
            }
            Transition n1 = (Transition) o1;
            Transition n2 = (Transition) o2;
            if (n1.min != n2.min) {
                return (new Character(n1.min).compareTo(n2.min));
            } else {
                if (n1.max != n2.max) {
                    return (new Character(n1.max).compareTo(n2.max));
                }
                // they have the same labels; try taint status...
                if (n1.taint != n2.taint) {
                    if (n1.taint == Transition.Taint.Untainted) {
                        return -1;
                    } else if (n2.taint == Transition.Taint.Untainted) {
                        return 1;
                    } else if (n1.taint == Transition.Taint.Indirectly) {
                        return -1;
                    } else if (n2.taint == Transition.Taint.Indirectly) {
                        return 1;
                    } else {
                        throw new RuntimeException("SNH");
                    }
                } else {
                    return 0;
                }
            }
        }
    }

    private class StateComparator<T>
        implements Comparator<T> {

        public int compare(T o1, T o2) {
            if (!(o1 instanceof State) || !(o2 instanceof State)) {
                throw new RuntimeException("SNH");
            }
            State n1 = (State) o1;
            State n2 = (State) o2;
            return new Integer(n1.number).compareTo(n2.number);
        }
    }

    /**
     * Checks whether there is a loop containing s. (This is sufficient since
     * there are never transitions to dead states.)
     */
    boolean isFinite(State s, HashSet<State> path) {
        path.add(s);
        for (Transition t : s.transitions)
            if (path.contains(t.to) || !isFinite(t.to, path))
                return false;
        path.remove(s);
        return true;
    }

    /**
     * Returns the strings that can be produced from s, returns false if more than
     * <code>limit</code> strings are found. <code>limit</code>&lt;0 means "infinite".
     */
    boolean getFiniteStrings(State s, HashSet<State> pathstates, HashSet<String> strings, StringBuilder path, int limit) {
        pathstates.add(s);
        for (Transition t : s.transitions) {
            if (pathstates.contains(t.to))
                return false;
            for (int n = t.min; n <= t.max; n++) {
                path.append((char) n);
                if (t.to.accept) {
                    strings.add(path.toString());
                    if (limit >= 0 && strings.size() > limit)
                        return false;
                }
                if (!getFiniteStrings(t.to, pathstates, strings, path, limit))
                    return false;
                path.deleteCharAt(path.length() - 1);
            }
        }
        pathstates.remove(s);
        return true;
    }

    /**
     * Adds transitions to explicit crash state to ensure that transition function is total.
     */
    void totalize() {
        State s = new State();
        s.transitions.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, s));
        for (State p : getStates()) {
            int maxi = Character.MIN_VALUE;
            for (Transition t : p.getSortedTransitions(false)) {
                if (t.min > maxi)
                    p.transitions.add(new Transition((char) maxi, (char) (t.min - 1), s));
                if (t.max + 1 > maxi)
                    maxi = t.max + 1;
            }
            if (maxi <= Character.MAX_VALUE)
                p.transitions.add(new Transition((char) maxi, Character.MAX_VALUE, s));
        }
    }

    /**
     * Reduces this automaton.
     * An automaton is "reduced" by combining overlapping and adjacent edge intervals with same destination.
     */
    public void reduce() {
        if (isSingleton())
            return;
        Set<State> states = getStates();
        setStateNumbers(states);
        for (State s : states) {
            List<Transition> st = s.getSortedTransitions(true);
            s.resetTransitions();
            State p = null;
            int min = -1, max = -1;
            for (Transition t : st) {
                if (p == t.to) {
                    if (t.min <= max + 1) {
                        if (t.max > max)
                            max = t.max;
                    } else {
                        if (p != null)
                            s.transitions.add(new Transition((char) min, (char) max, p));
                        min = t.min;
                        max = t.max;
                    }
                } else {
                    if (p != null)
                        s.transitions.add(new Transition((char) min, (char) max, p));
                    p = t.to;
                    min = t.min;
                    max = t.max;
                }
            }
            if (p != null)
                s.transitions.add(new Transition((char) min, (char) max, p));
        }
    }

    /**
     * gets sorted array of all interval start points.
     */
    char[] getStartPoints() {
        Set<Character> pointset = new HashSet<>();
        for (State s : getStates()) {
            pointset.add(Character.MIN_VALUE);
            for (Transition t : s.transitions) {
                pointset.add(t.min);
                if (t.max < Character.MAX_VALUE)
                    pointset.add((char) (t.max + 1));
            }
        }
        char[] points = new char[pointset.size()];
        int n = 0;
        for (Character m : pointset)
            points[n++] = m;
        Arrays.sort(points);
        return points;
    }

    Set<State> getLiveStates(Set<State> states) {
        HashMap<State, Set<State>> map = new HashMap<>();
        for (State s : states)
            map.put(s, new HashSet<State>());
        for (State s : states)
            for (Transition t : s.transitions)
                map.get(t.to).add(s);
        Set<State> live = new HashSet<>(getAcceptStates());
        LinkedList<State> worklist = new LinkedList<>(live);
        while (worklist.size() > 0) {
            State s = worklist.removeFirst();
            for (State p : map.get(s))
                if (!live.contains(p)) {
                    live.add(p);
                    worklist.add(p);
                }
        }
        return live;
    }

    /**
     * Removes transitions to dead states and calls {@link #reduce()}
     * (a state is "dead" if no accept state is reachable from it).
     */
    public void removeDeadTransitions() {
        if (isSingleton())
            return;
        Set<State> states = getStates();
        Set<State> live = getLiveStates(states);
        for (State s : states) {
            Set<Transition> st = s.transitions;
            s.resetTransitions();
            for (Transition t : st)
                if (live.contains(t.to))
                    s.transitions.add(t);
        }
        reduce();
    }

    /**
     * Returns sorted array of transitions for each state (and sets state numbers).
     */
    static Transition[][] getSortedTransitions(Set<State> states) {
        setStateNumbers(states);
        Transition[][] transitions = new Transition[states.size()][];
        for (State s : states)
            transitions[s.number] = s.getSortedTransitionArray(false);
        return transitions;
    }

    /**
     * Returns new (deterministic) automaton with the empty language.
     */
    public static Automaton makeEmpty() {
        Automaton a = new Automaton();
        State s = new State();
        a.initial = s;
        a.deterministic = true;
        return a;
    }

    /**
     * Returns new (deterministic) automaton that accepts only the empty string.
     */
    public static Automaton makeEmptyString() {
        Automaton a = new Automaton();
        a.singleton = "";
        a.deterministic = true;
        return a;
    }

    /**
     * Returns new (deterministic) automaton that accepts all strings.
     */
    public static Automaton makeAnyString(Transition.Taint taint) {
        Automaton a = new Automaton();
        State s = new State();
        a.initial = s;
        s.accept = true;
        s.transitions.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, s, taint));
        a.deterministic = true;
        return a;
    }

    /**
     * Returns new (deterministic) automaton that accepts a single character of the given value.
     */
    public static Automaton makeChar(char c) {
        Automaton a = new Automaton();
        a.singleton = Character.toString(c);
        a.deterministic = true;
        return a;
    }

    /**
     * Returns new (deterministic) automaton that accepts a single char
     * whose value is in the given interval (including both end points).
     */
    public static Automaton makeCharRange(char min, char max) {
        if (min == max)
            return makeChar(min);
        Automaton a = new Automaton();
        State s1 = new State();
        State s2 = new State();
        a.initial = s1;
        s2.accept = true;
        if (min <= max)
            s1.transitions.add(new Transition(min, max, s2));
        a.deterministic = true;
        return a;
    }

    /**
     * Constructs sub-automaton corresponding to decimal numbers of
     * length x.substring(n).length().
     */
    private static State anyOfRightLength(String x, int n) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else
            s.addTransition(new Transition('0', '9', anyOfRightLength(x, n + 1)));
        return s;
    }

    /**
     * Constructs sub-automaton corresponding to decimal numbers of value
     * at least x.substring(n) and length x.substring(n).length().
     */
    private static State atLeast(String x, int n, Collection<State> initials, boolean zeros) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else {
            if (zeros)
                initials.add(s);
            char c = x.charAt(n);
            s.addTransition(new Transition(c, atLeast(x, n + 1, initials, zeros && c == '0')));
            if (c < '9')
                s.addTransition(new Transition((char) (c + 1), '9', anyOfRightLength(x, n + 1)));
        }
        return s;
    }

    /**
     * Constructs sub-automaton corresponding to decimal numbers of value
     * at most x.substring(n) and length x.substring(n).length().
     */
    private static State atMost(String x, int n) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else {
            char c = x.charAt(n);
            s.addTransition(new Transition(c, atMost(x, (char) n + 1)));
            if (c > '0')
                s.addTransition(new Transition('0', (char) (c - 1), anyOfRightLength(x, n + 1)));
        }
        return s;
    }

    /**
     * Constructs sub-automaton corresponding to decimal numbers of value
     * between x.substring(n) and y.substring(n) and of
     * length x.substring(n).length() (which must be equal to y.substring(n).length()).
     */
    private static State between(String x, String y, int n, Collection<State> initials, boolean zeros) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else {
            if (zeros)
                initials.add(s);
            char cx = x.charAt(n);
            char cy = y.charAt(n);
            if (cx == cy)
                s.addTransition(new Transition(cx, between(x, y, n + 1, initials, zeros && cx == '0')));
            else { // cx<cy
                s.addTransition(new Transition(cx, atLeast(x, n + 1, initials, zeros && cx == '0')));
                s.addTransition(new Transition(cy, atMost(y, n + 1)));
                if (cx + 1 < cy)
                    s.addTransition(new Transition((char) (cx + 1), (char) (cy - 1), anyOfRightLength(x, n + 1)));
            }
        }
        return s;
    }

    /**
     * Expands singleton representation to normal representation.
     */
    void expandSingleton() {
        if (isSingleton()) {
            State p = new State();
            initial = p;
            for (int i = 0; i < singleton.length(); i++) {
                State q = new State();
                p.transitions.add(new Transition(singleton.charAt(i), q));
                p = q;
            }
            p.accept = true;
            deterministic = true;
            singleton = null;
        }
    }

    /**
     * Returns new (deterministic) automaton that accepts the single given string.
     * <p/>
     * Complexity: constant.
     */
    public static Automaton makeString(String s) {
        Automaton a = new Automaton();
        a.singleton = s;
        a.deterministic = true;
        return a;
    }

    /**
     * Returns new automaton that accepts the concatenation of the languages of
     * this and the given automaton.
     * <p/>
     * Complexity: linear in number of states.
     */
    public Automaton concatenate(Automaton a) {
        if (isSingleton() && a.isSingleton())
            return makeString(singleton + a.singleton);
        a = a.cloneExpanded();
        Automaton b = cloneExpanded();
        for (State s : b.getAcceptStates()) {
            s.accept = false;
            s.addEpsilon(a.initial);
        }
        b.deterministic = false;
        b.checkMinimizeAlways();
        return b;
    }

    /**
     * Returns new automaton that accepts the concatenation of the languages of
     * the given automata.
     * <p/>
     * Complexity: linear in total number of states.
     */
    static public Automaton concatenate(List<Automaton> l) {
        if (l.isEmpty())
            return makeEmptyString();
        boolean all_singleton = true;
        for (Automaton a : l)
            if (!a.isSingleton()) {
                all_singleton = false;
                break;
            }
        if (all_singleton) {
            StringBuilder b = new StringBuilder();
            for (Automaton a : l)
                b.append(a.singleton);
            return makeString(b.toString());
        } else {
            for (Automaton a : l)
                if (a.isEmpty())
                    return makeEmpty();
            Automaton b = l.get(0).cloneExpanded();
            Set<State> ac = b.getAcceptStates();
            boolean first = true;
            for (Automaton a : l)
                if (first)
                    first = false;
                else {
                    if (a.isEmptyString())
                        continue;
                    Automaton aa = a.cloneExpanded();
                    Set<State> ns = aa.getAcceptStates();
                    for (State s : ac) {
                        s.accept = false;
                        s.addEpsilon(aa.initial);
                        //noinspection ConstantConditions
                        if (s.accept) {
                            ns.add(s);
                        }
                    }
                    ac = ns;
                }
            b.deterministic = false;
            b.checkMinimizeAlways();
            return b;
        }
    }

    /**
     * Returns new automaton that accepts the Kleene star (zero or more
     * concatenated repetitions) of the language of this automaton.
     * <p/>
     * Complexity: linear in number of states.
     */
    public Automaton repeat() {
        Automaton a = cloneExpanded();
        State s = new State();
        s.accept = true;
        s.addEpsilon(a.initial);
        for (State p : a.getAcceptStates())
            p.addEpsilon(s);
        a.initial = s;
        a.deterministic = false;
        a.checkMinimizeAlways();
        return a;
    }

    /**
     * Returns new (deterministic) automaton that accepts the complement of the
     * language of this automaton.
     * <p/>
     * Complexity: linear in number of states (if already deterministic).
     */
    public Automaton complement() {
        Automaton a = cloneExpanded();
        a.determinize();
        a.totalize();
        for (State p : a.getStates())
            p.accept = !p.accept;
        a.removeDeadTransitions();
        return a;
    }

    /**
     * Returns new (deterministic) automaton that accepts the intersection of
     * the languages of this and the given automaton. As a side-effect, both
     * this and the given automaton are determinized, if not already
     * deterministic.
     * <p/>
     * Complexity: quadratic in number of states (if already deterministic).
     */
    public Automaton intersection(Automaton a) {
        if (isSingleton()) {
            if (a.run(singleton))
                return clone();
            else
                return makeEmpty();
        }
        if (a.isSingleton()) {
            if (run(a.singleton))
                return a.clone();
            else
                return makeEmpty();
        }
        determinize();
        a.determinize();
        if (a == this)
            return clone();
        Transition[][] transitions1 = getSortedTransitions(getStates());
        Transition[][] transitions2 = getSortedTransitions(a.getStates());
        Automaton c = new Automaton();
        LinkedList<StatePair> worklist = new LinkedList<>();
        HashMap<StatePair, StatePair> newstates = new HashMap<>();
        State s = new State();
        c.initial = s;
        StatePair p = new StatePair(s, initial, a.initial);
        worklist.add(p);
        newstates.put(p, p);
        while (worklist.size() > 0) {
            p = worklist.removeFirst();
            p.s.accept = p.s1.accept && p.s2.accept;
            Transition[] t1 = transitions1[p.s1.number];
            Transition[] t2 = transitions2[p.s2.number];
            for (int n1 = 0, n2 = 0; n1 < t1.length && n2 < t2.length; ) {
                if (t1[n1].max < t2[n2].min)
                    n1++;
                else if (t2[n2].max < t1[n1].min)
                    n2++;
                else {
                    StatePair q = new StatePair(t1[n1].to, t2[n2].to);
                    StatePair r = newstates.get(q);
                    if (r == null) {
                        q.s = new State();
                        worklist.add(q);
                        newstates.put(q, q);
                        r = q;
                    }
                    char min = t1[n1].min > t2[n2].min ? t1[n1].min : t2[n2].min;
                    char max = t1[n1].max < t2[n2].max ? t1[n1].max : t2[n2].max;
                    p.s.transitions.add(new Transition(min, max, r.s));
                    if (t1[n1].max < t2[n2].max)
                        n1++;
                    else
                        n2++;
                }
            }
        }
        c.deterministic = true;
        c.removeDeadTransitions();
        c.checkMinimizeAlways();
        return c;
    }

   /**
     * Returns new automaton that accepts the union of the languages of this and
     * the given automaton.
     * <p/>
     * Complexity: linear in number of states.
     */
    public Automaton union(Automaton a) {
        if ((isSingleton() && a.isSingleton() && singleton.equals(a.singleton)) || a == this)
            return clone();
        a = a.cloneExpanded();
        Automaton b = cloneExpanded();
        State s = new State();
        s.addEpsilon(a.initial);
        s.addEpsilon(b.initial);
        a.initial = s;
        a.deterministic = false;
        a.checkMinimizeAlways();
        return a;
    }

    /**
     * Determinizes this automaton.
     * <p/>
     * Complexity: exponential in number of states.
     */
    public void determinize() {
        if (deterministic || isSingleton())
            return;
        Set<State> initialset = new HashSet<>();
        initialset.add(initial);
        determinize(initialset);
    }

    /**
     * Determinizes this automaton using the given set of initial states.
     */
    private void determinize(Set<State> initialset) {
        char[] points = getStartPoints();
        // subset construction
        Map<Set<State>, Set<State>> sets = new HashMap<>();
        LinkedList<Set<State>> worklist = new LinkedList<>();
        Map<Set<State>, State> newstate = new HashMap<>();
        sets.put(initialset, initialset);
        worklist.add(initialset);
        initial = new State();
        newstate.put(initialset, initial);
        while (worklist.size() > 0) {
            Set<State> s = worklist.removeFirst();
            State r = newstate.get(s);
            for (State q : s)
                if (q.accept) {
                    r.accept = true;
                    break;
                }
            for (int n = 0; n < points.length; n++) {
                Set<State> p = new HashSet<>();
                for (State q : s)
                    for (Transition t : q.transitions)
                        if (t.min <= points[n] && points[n] <= t.max)
                            p.add(t.to);
                if (!sets.containsKey(p)) {
                    sets.put(p, p);
                    worklist.add(p);
                    newstate.put(p, new State());
                }
                State q = newstate.get(p);
                char min = points[n];
                char max;
                if (n + 1 < points.length)
                    max = (char) (points[n + 1] - 1);
                else
                    max = Character.MAX_VALUE;
                r.transitions.add(new Transition(min, max, q));
            }
        }
        deterministic = true;
        removeDeadTransitions();
    }

    /**
     * Minimizes (and determinizes if not already deterministic) this automaton.
     *
     * @see #setMinimization(int)
     */
    public void minimize() {
        if (!isSingleton()) {
            switch (minimization) {
                case MINIMIZE_HUFFMAN:
                    minimizeHuffman();
                    break;
                case MINIMIZE_BRZOZOWSKI:
                    minimizeBrzozowski();
                    break;
                default:
                    minimizeHopcroft();
            }
        }
        // recompute hash code
        hash_code = getNumberOfStates() * 3 + getNumberOfTransitions() * 2;
        if (hash_code == 0)
            hash_code = 1;
    }

    private boolean statesAgree(Transition[][] transitions, boolean[][] mark, int n1, int n2) {
        Transition[] t1 = transitions[n1];
        Transition[] t2 = transitions[n2];
        for (int k1 = 0, k2 = 0; k1 < t1.length && k2 < t2.length; ) {
            if (t1[k1].max < t2[k2].min)
                k1++;
            else if (t2[k2].max < t1[k1].min)
                k2++;
            else {
                int m1 = t1[k1].to.number;
                int m2 = t2[k2].to.number;
                if (m1 > m2) {
                    int t = m1;
                    m1 = m2;
                    m2 = t;
                }
                if (mark[m1][m2])
                    return false;
                if (t1[k1].max < t2[k2].max)
                    k1++;
                else
                    k2++;
            }
        }
        return true;
    }

    private void addTriggers(Transition[][] transitions, ArrayList<ArrayList<HashSet<IntPair>>> triggers, int n1, int n2) {
        Transition[] t1 = transitions[n1];
        Transition[] t2 = transitions[n2];
        for (int k1 = 0, k2 = 0; k1 < t1.length && k2 < t2.length; ) {
            if (t1[k1].max < t2[k2].min)
                k1++;
            else if (t2[k2].max < t1[k1].min)
                k2++;
            else {
                if (t1[k1].to != t2[k2].to) {
                    int m1 = t1[k1].to.number;
                    int m2 = t2[k2].to.number;
                    if (m1 > m2) {
                        int t = m1;
                        m1 = m2;
                        m2 = t;
                    }
                    if (triggers.get(m1).get(m2) == null)
                        triggers.get(m1).set(m2, new HashSet<IntPair>());
                    triggers.get(m1).get(m2).add(new IntPair(n1, n2));
                }
                if (t1[k1].max < t2[k2].max)
                    k1++;
                else
                    k2++;
            }
        }
    }

    private void markPair(boolean[][] mark, ArrayList<ArrayList<HashSet<IntPair>>> triggers, int n1, int n2) {
        mark[n1][n2] = true;
        if (triggers.get(n1).get(n2) != null) {
            for (IntPair p : triggers.get(n1).get(n2)) {
                int m1 = p.n1;
                int m2 = p.n2;
                if (m1 > m2) {
                    int t = m1;
                    m1 = m2;
                    m2 = t;
                }
                if (!mark[m1][m2])
                    markPair(mark, triggers, m1, m2);
            }
        }
    }

    private static <T> void initialize(ArrayList<T> list, int size) {
        for (int i = 0; i < size; i++)
            list.add(null);
    }

    /**
     * Minimize using Huffman's algorithm.
     */
    private void minimizeHuffman() {
        determinize();
        totalize();
        Set<State> ss = getStates();
        Transition[][] transitions = new Transition[ss.size()][];
        State[] states = ss.toArray(new State[0]);
        boolean[][] mark = new boolean[states.length][states.length];
        ArrayList<ArrayList<HashSet<IntPair>>> triggers = new ArrayList<>();
        for (State state : states) {
            ArrayList<HashSet<IntPair>> v = new ArrayList<>();
            initialize(v, states.length);
            triggers.add(v);
        }
        // initialize marks based on acceptance status and find transition arrays
        for (int n1 = 0; n1 < states.length; n1++) {
            states[n1].number = n1;
            transitions[n1] = states[n1].getSortedTransitionArray(false);
            for (int n2 = n1 + 1; n2 < states.length; n2++)
                if (states[n1].accept != states[n2].accept)
                    mark[n1][n2] = true;
        }
        // for all pairs, see if states agree
        for (int n1 = 0; n1 < states.length; n1++)
            for (int n2 = n1 + 1; n2 < states.length; n2++)
                if (!mark[n1][n2]) {
                    if (statesAgree(transitions, mark, n1, n2))
                        addTriggers(transitions, triggers, n1, n2);
                    else
                        markPair(mark, triggers, n1, n2);
                }
        // assign equivalence class numbers to states
        int numclasses = 0;
        for (State state : states) state.number = -1;
        for (int n1 = 0; n1 < states.length; n1++)
            if (states[n1].number == -1) {
                states[n1].number = numclasses;
                for (int n2 = n1 + 1; n2 < states.length; n2++)
                    if (!mark[n1][n2])
                        states[n2].number = numclasses;
                numclasses++;
            }
        // make a new state for each equivalence class
        State[] newstates = new State[numclasses];
        for (int n = 0; n < numclasses; n++)
            newstates[n] = new State();
        // select a class representative for each class and find the new initial
        // state
        for (int n = 0; n < states.length; n++) {
            newstates[states[n].number].number = n;
            if (states[n] == initial)
                initial = newstates[states[n].number];
        }
        // build transitions and set acceptance
        for (int n = 0; n < numclasses; n++) {
            State s = newstates[n];
            s.accept = states[s.number].accept;
            for (Transition t : states[s.number].transitions)
                s.transitions.add(new Transition(t.min, t.max, newstates[t.to.number]));
        }
        removeDeadTransitions();
    }

    /**
     * Minimize using Brzozowski's algorithm.
     */
    private void minimizeBrzozowski() {
        if (isSingleton())
            return;
        determinize(reverse());
        determinize(reverse());
    }

    /**
     * Minimize using Hopcroft's algorithm.
     */
    private void minimizeHopcroft() {
        determinize();
        Set<Transition> tr = initial.getTransitions();
        if (tr.size() == 1) {
            Transition t = tr.iterator().next();
            if (t.to == initial && t.min == Character.MIN_VALUE && t.max == Character.MAX_VALUE)
                return;
        }
        totalize();
        // make arrays for numbered states and effective alphabet
        Set<State> ss = getStates();
        State[] states = new State[ss.size()];
        int number = 0;
        for (State q : ss) {
            states[number] = q;
            q.number = number++;
        }
        char[] sigma = getStartPoints();
        // initialize data structures
        ArrayList<ArrayList<LinkedList<State>>> reverse = new ArrayList<>();
        for (State state : states) {
            ArrayList<LinkedList<State>> v = new ArrayList<>();
            initialize(v, sigma.length);
            reverse.add(v);
        }
        boolean[][] reverse_nonempty = new boolean[states.length][sigma.length];
        ArrayList<LinkedList<State>> partition = new ArrayList<>();
        initialize(partition, states.length);
        int[] block = new int[states.length];
        StateList[][] active = new StateList[states.length][sigma.length];
        StateListNode[][] active2 = new StateListNode[states.length][sigma.length];
        LinkedList<IntPair> pending = new LinkedList<>();
        boolean[][] pending2 = new boolean[sigma.length][states.length];
        ArrayList<State> split = new ArrayList<>();
        boolean[] split2 = new boolean[states.length];
        ArrayList<Integer> refine = new ArrayList<>();
        boolean[] refine2 = new boolean[states.length];
        ArrayList<ArrayList<State>> splitblock = new ArrayList<>();
        initialize(splitblock, states.length);
        for (int q = 0; q < states.length; q++) {
            splitblock.set(q, new ArrayList<State>());
            partition.set(q, new LinkedList<State>());
            for (int a = 0; a < sigma.length; a++) {
                reverse.get(q).set(a, new LinkedList<State>());
                active[q][a] = new StateList();
            }
        }
        // find initial partition and reverse edges
        for (State qq : states) {
            int j;
            if (qq.accept)
                j = 0;
            else
                j = 1;
            partition.get(j).add(qq);
            block[qq.number] = j;
            for (int a = 0; a < sigma.length; a++) {
                char aa = sigma[a];
                State p = qq.step(aa);
                reverse.get(p.number).get(a).add(qq);
                reverse_nonempty[p.number][a] = true;
            }
        }
        // initialize active sets
        for (int j = 0; j <= 1; j++)
            for (int a = 0; a < sigma.length; a++)
                for (State qq : partition.get(j))
                    if (reverse_nonempty[qq.number][a])
                        active2[qq.number][a] = active[j][a].add(qq);
        // initialize pending
        for (int a = 0; a < sigma.length; a++) {
            int a0 = active[0][a].size;
            int a1 = active[1][a].size;
            int j;
            if (a0 <= a1)
                j = 0;
            else
                j = 1;
            pending.add(new IntPair(j, a));
            pending2[a][j] = true;
        }
        // process pending until fixed point
        int k = 2;
        while (!pending.isEmpty()) {
            IntPair ip = pending.removeFirst();
            int p = ip.n1;
            int a = ip.n2;
            pending2[a][p] = false;
            // find states that need to be split off their blocks
            for (StateListNode m = active[p][a].first; m != null; m = m.next)
                for (State s : reverse.get(m.q.number).get(a))
                    if (!split2[s.number]) {
                        split2[s.number] = true;
                        split.add(s);
                        int j = block[s.number];
                        splitblock.get(j).add(s);
                        if (!refine2[j]) {
                            refine2[j] = true;
                            refine.add(j);
                        }
                    }
            // refine blocks
            for (int j : refine) {
                if (splitblock.get(j).size() < partition.get(j).size()) {
                    LinkedList<State> b1 = partition.get(j);
                    LinkedList<State> b2 = partition.get(k);
                    for (State s : splitblock.get(j)) {
                        b1.remove(s);
                        b2.add(s);
                        block[s.number] = k;
                        for (int c = 0; c < sigma.length; c++) {
                            StateListNode sn = active2[s.number][c];
                            if (sn != null && sn.sl == active[j][c]) {
                                sn.remove();
                                active2[s.number][c] = active[k][c].add(s);
                            }
                        }
                    }
                    // update pending
                    for (int c = 0; c < sigma.length; c++) {
                        int aj = active[j][c].size;
                        int ak = active[k][c].size;
                        if (!pending2[c][j] && 0 < aj && aj <= ak) {
                            pending2[c][j] = true;
                            pending.add(new IntPair(j, c));
                        } else {
                            pending2[c][k] = true;
                            pending.add(new IntPair(k, c));
                        }
                    }
                    k++;
                }
                for (State s : splitblock.get(j))
                    split2[s.number] = false;
                refine2[j] = false;
                splitblock.get(j).clear();
            }
            split.clear();
            refine.clear();
        }
        // make a new state for each equivalence class, set initial state
        State[] newstates = new State[k];
        for (int n = 0; n < newstates.length; n++) {
            State s = new State();
            newstates[n] = s;
            for (State q : partition.get(n)) {
                if (q == initial)
                    initial = s;
                s.accept = q.accept;
                s.number = q.number; // select representative
                q.number = n;
            }
        }
        // build transitions and set acceptance
        for (State s : newstates) {
            s.accept = states[s.number].accept;
            for (Transition t : states[s.number].transitions)
                s.transitions.add(new Transition(t.min, t.max, newstates[t.to.number]));
        }
        removeDeadTransitions();
    }

    /**
     * Reverses the language of this (non-singleton) automaton while returning
     * the set of new initial states. (Used for
     * <code>minimizeBrzozowski()</code>)
     */
    private Set<State> reverse() {
        // reverse all edges
        HashMap<State, HashSet<Transition>> m = new HashMap<>();
        Set<State> states = getStates();
        Set<State> accept = getAcceptStates();
        for (State r : states) {
            m.put(r, new HashSet<Transition>());
            r.accept = false;
        }
        for (State r : states)
            for (Transition t : r.getTransitions())
                m.get(t.to).add(new Transition(t.min, t.max, r));
        for (State r : states)
            r.transitions = m.get(r);
        // make new initial+final states
        initial.accept = true;
        initial = new State();
        for (State r : accept)
            initial.addEpsilon(r); // ensures that all initial states are reachable
        deterministic = false;
        return accept;
    }

    /**
     * Adds epsilon transitions to this automaton.
     * This method adds extra character interval transitions that are equivalent to the given
     * set of epsilon transitions.
     *
     * @param pairs collection of {@link StatePair} objects representing pairs of source/destination states
     *              where epsilon transitions should be added
     */
    public void addEpsilons(Collection<StatePair> pairs) {
        expandSingleton();
        HashMap<State, HashSet<State>> forward = new HashMap<>();
        HashMap<State, HashSet<State>> back = new HashMap<>();
        for (StatePair p : pairs) {
            HashSet<State> to = forward.get(p.s1);
            if (to == null) {
                to = new HashSet<>();
                forward.put(p.s1, to);
            }
            to.add(p.s2);
            HashSet<State> from = back.get(p.s2);
            if (from == null) {
                from = new HashSet<>();
                back.put(p.s2, from);
            }
            from.add(p.s1);
        }
        // calculate epsilon closure
        LinkedList<StatePair> worklist = new LinkedList<>(pairs);
        HashSet<StatePair> workset = new HashSet<>(pairs);
        while (!worklist.isEmpty()) {
            StatePair p = worklist.removeFirst();
            workset.remove(p);
            HashSet<State> to = forward.get(p.s2);
            HashSet<State> from = back.get(p.s1);
            if (to != null) {
                for (State s : to) {
                    StatePair pp = new StatePair(p.s1, s);
                    if (!pairs.contains(pp)) {
                        pairs.add(pp);
                        forward.get(p.s1).add(s);
                        back.get(s).add(p.s1);
                        worklist.add(pp);
                        workset.add(pp);
                        if (from != null) {
                            for (State q : from) {
                                StatePair qq = new StatePair(q, p.s1);
                                if (!workset.contains(qq)) {
                                    worklist.add(qq);
                                    workset.add(qq);
                                }
                            }
                        }
                    }
                }
            }
        }
        // add transitions
        for (StatePair p : pairs)
            p.s1.addEpsilon(p.s2);
        deterministic = false;
        checkMinimizeAlways();
    }

    /**
     * Returns true if the given string is accepted by this automaton. As a
     * side-effect, this automaton is determinized if not already deterministic.
     * <p/>
     * Complexity: linear in length of string (if automaton is already
     * deterministic) and in number of transitions.
     * <p/>
     * <b>Note:</b> to obtain maximum speed, use the {@link RunAutomaton} class.
     */
    public boolean run(String s) {
        if (isSingleton())
            return s.equals(singleton);
        determinize();
        State p = initial;
        for (int i = 0; i < s.length(); i++) {
            State q = p.step(s.charAt(i));
            if (q == null)
                return false;
            p = q;
        }
        return p.accept;
    }

    /**
     * Returns number of states in this automaton.
     */
    public int getNumberOfStates() {
        if (isSingleton())
            return singleton.length() + 1;
        return getStates().size();
    }

    /**
     * Returns number of transitions in this automaton. This number is counted
     * as the total number of edges, where one edge may be a character interval.
     */
    public int getNumberOfTransitions() {
        if (isSingleton())
            return singleton.length();
        int c = 0;
        for (State s : getStates())
            c += s.transitions.size();
        return c;
    }

    /**
     * Returns true if this automaton accepts the empty string and nothing else.
     */
    public boolean isEmptyString() {
        if (isSingleton())
            return singleton.length() == 0;
        else
            return initial.accept && initial.transitions.isEmpty();
    }

    /**
     * Returns true if this automaton accepts no strings.
     */
    public boolean isEmpty() {
        return !isSingleton() && !initial.accept && initial.transitions.isEmpty();
    }

    /**
     * Returns true if the language of this automaton is finite.
     */
    public boolean isFinite() {
        return isSingleton() || isFinite(initial, new HashSet<State>());
    }

    /**
     * Returns set of accepted strings, assuming this automaton has a finite
     * language. If the language is not finite, null is returned.
     */
    public Set<String> getFiniteStrings() {
        HashSet<String> strings = new HashSet<>();
        if (isSingleton())
            strings.add(singleton);
        else if (!getFiniteStrings(initial, new HashSet<State>(), strings, new StringBuilder(), -1))
            return null;
        return strings;
    }

    /**
     * Returns the longest string that is a prefix of all accepted strings and
     * visits each state at most once.
     *
     * @return common prefix
     */
    public String getCommonPrefix() {
        if (isSingleton())
            return singleton;
        StringBuilder b = new StringBuilder();
        HashSet<State> visited = new HashSet<>();
        State s = initial;
        boolean done;
        do {
            done = true;
            visited.add(s);
            if (!s.accept && s.transitions.size() == 1) {
                Transition t = s.transitions.iterator().next();
                if (t.min == t.max && !visited.contains(t.to)) {
                    b.append(t.min);
                    s = t.to;
                    done = false;
                }
            }
        } while (!done);
        return b.toString();
    }

    // analogous to getCommonPrefix, but with reverse and with bigger effort
    public String getCommonSuffix() {
        if (isSingleton())
            return singleton;
        StringBuilder b = new StringBuilder();
        HashSet<State> visited = new HashSet<>();

        Set<State> acceptStates = getAcceptStates();
        if (acceptStates.size() != 1) {
            return b.toString();
        }

        Map<State, Set<State>> reverse = this.getReverseTransitions(this.getStates());

        State s = acceptStates.iterator().next();
        boolean done;
        do {
            done = true;
            visited.add(s);
            Set<State> fromSet = reverse.get(s);

            // if there is exactly one successor
            if (fromSet.size() == 1) {
                State from = fromSet.iterator().next();
                Character c = null;
                // for all transitions between this successor and the current node...
                for (Transition fromTrans : from.transitions) {
                    if (fromTrans.to.equals(s)) {
                        if (fromTrans.min == fromTrans.max && !visited.contains(from)) {
                            if (c == null) {
                                c = fromTrans.min;
                            } else if (fromTrans.min == c) {
                                // match, do nothing
                            } else {
                                // mismatch
                                c = null;
                                break;
                            }
                        }
                    }
                }
                if (c != null) {
                    b.insert(0, c.charValue());
                    s = from;
                    done = false;
                }
            } else {
                // there is more than one successor;
                // try one last step
                Character c = null;
                for (State from : fromSet) {

                    for (Transition fromTrans : from.transitions) {
                        if (fromTrans.to.equals(s)) {
                            if (fromTrans.min == fromTrans.max && !visited.contains(from)) {
                                if (c == null) {
                                    c = fromTrans.min;
                                } else if (fromTrans.min == c) {
                                    // match, do nothing
                                } else {
                                    // mismatch
                                    c = null;
                                    break;
                                }
                            }
                        }
                    }
                    if (c == null) {
                        break;
                    }
                }
                if (c != null) {
                    b.insert(0, c.charValue());
                }
            }
        } while (!done);

        return b.toString();
    }

    /**
     * Returns true if the language of this automaton is a subset of the
     * language of the given automaton. Implemented using
     * <code>this.intersection(a.complement()).isEmpty()</code>.
     */
    public boolean subsetOf(Automaton a) {
        if (a == this)
            return true;
        if (isSingleton()) {
            if (a.isSingleton())
                return singleton.equals(a.singleton);
            return a.run(singleton);
        }
        return intersection(a.complement()).isEmpty();
    }

    /**
     * Returns true if the language of this automaton is equal to the language
     * of the given automaton. Implemented using <code>hashCode</code> and
     * <code>subsetOf</code>.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Automaton))
            return false;
        Automaton a = (Automaton) obj;
        if (isSingleton() && a.isSingleton())
            return singleton.equals(a.singleton);
        return hashCode() == a.hashCode() && subsetOf(a) && a.subsetOf(this);
    }

    /**
     * Returns hash code for this automaton. The hash code is based on the
     * number of states and transitions in the minimized automaton.
     */
    @Override
    public int hashCode() {
        if (hash_code == 0)
            minimize();
        return hash_code;
    }

    /**
     * Returns a string representation of this automaton.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        Set<State> states = getStates();
        setStateNumbers(states);
        b.append("initial state: ").append(initial.number).append("\n");
        for (State s : states)
            b.append(s.toString());
        return b.toString();
    }

    /**
     * Returns <a href="http://www.research.att.com/sw/tools/graphviz/" target="_top">Graphviz Dot</a>
     * representation of this automaton.
     */
    public String toDot() {
        StringBuilder b = new StringBuilder("digraph Automaton {\n");
        b.append("  rankdir = LR;\n");
        Set<State> states = getStates();
        setStateNumbers(states);
        for (State s : states) {
            b.append("  ").append(s.number);
            String color = "";
            if (s.accept)
                b.append(" [shape=doublecircle,label=\"\"" + color + "];\n");
            else
                b.append(" [shape=circle,label=\"\"" + color + "];\n");
            if (s == initial) {
                b.append("  initial [shape=plaintext,label=\"\"];\n");
                b.append("  initial -> ").append(s.number).append("\n");
            }
            for (Transition t : s.transitions) {
                b.append("  ").append(s.number);
                t.appendDot(b);
            }
        }
        return b.append("}\n").toString();
    }

    public String toDotUnique() {
        StringBuilder b = new StringBuilder("digraph Automaton {\n");
        b.append("  rankdir = LR;\n");
        Set<State> states = getStates();
        setStateNumbersUnique();
        List<State> stateList = new LinkedList<>(states);
        Collections.sort(stateList, new StateComparator<State>());
        for (State s : stateList) {
            b.append("  ").append(s.number);
            String color = "";
            if (s.accept)
                b.append(" [shape=doublecircle,label=\"\"" + color + "];\n");
            else
                b.append(" [shape=circle,label=\"\"" + color + "];\n");
            if (s == initial) {
                b.append("  initial [shape=plaintext,label=\"\"];\n");
                b.append("  initial -> ").append(s.number).append("\n");
            }
            List<Transition> successors = new LinkedList<>(s.transitions);
            Collections.sort(successors, new TransComparator<Transition>());
            for (Transition t : successors) {
                b.append("  ").append(s.number);
                t.appendDot(b);
            }
        }
        return b.append("}\n").toString();
    }

    // returns the textual input representation used by fsmtools (AT&T)
    public String toFsmTools() {

        StringBuilder b = new StringBuilder();
        Set<State> states = getStates();
        setStateNumbers(states);
        states.remove(this.initial);
        List<State> stateList = new LinkedList<>(states);
        stateList.add(0, this.initial);     // we want to print the initial state first
        Set<State> acceptStates = new HashSet<>();
        for (State s : stateList) {
            if (s.accept) {
                acceptStates.add(s);
            }
            for (Transition t : s.transitions) {
                if (t.min != t.max) {
                    throw new RuntimeException("not supported yet");
                }
                b.append(s.number).append(" ").append(t.to.number);
                StringBuilder temp = new StringBuilder();
                Transition.appendCharString(t.min, temp);
                b.append(" ").append(temp.toString()).append("\n");
            }
        }
        for (State s : acceptStates) {
            b.append(s.number).append("\n");
        }

        return b.toString();
    }

    // constructs an Automaton from the given fsmtools file (textual representation)
    public static Automaton fromFsmTools(String filename) {

        Automaton retMe = new Automaton();
        try {

            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;

            Map<String, State> string2State = new HashMap<>();
            State initial = null;

            Set<String> acceptStates = new HashSet<>();

            while ((line = reader.readLine()) != null) {

                StringTokenizer tokenizer = new StringTokenizer(line);
                if (tokenizer.countTokens() == 3) {
                    String sourceString = tokenizer.nextToken();
                    String destString = tokenizer.nextToken();
                    String transString = tokenizer.nextToken();

                    State sourceState = string2State.get(sourceString);
                    if (sourceState == null) {
                        sourceState = new State();
                        string2State.put(sourceString, sourceState);
                    }

                    State destState = string2State.get(destString);
                    if (destState == null) {
                        destState = new State();
                        string2State.put(destString, destState);
                    }

                    char transChar = Transition.reverseCharString(transString);

                    sourceState.addTransition(new Transition(transChar, destState));

                    if (initial == null) {
                        initial = sourceState;
                        retMe.initial = initial;
                    }
                } else if (tokenizer.countTokens() == 1) {

                    // collect accept states
                    String accept = tokenizer.nextToken();
                    acceptStates.add(accept);
                }
            }

            // set accept flag for accept states
            for (String acceptString : acceptStates) {
                State acceptState = string2State.get(acceptString);
                if (acceptState == null) {
                    throw new RuntimeException("SNH");
                }
                acceptState.accept = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        return retMe;
    }

    /**
     * Returns a clone of this automaton, expands if singleton.
     */
    Automaton cloneExpanded() {
        Automaton a = clone();
        a.expandSingleton();
        return a;
    }

    /**
     * Returns a clone of this automaton.
     */
    @Override
    public Automaton clone() {
        try {
            Automaton a = (Automaton) super.clone();
            if (!isSingleton()) {
                HashMap<State, State> m = new HashMap<>();
                Set<State> states = getStates();
                for (State s : states)
                    m.put(s, new State());
                for (State s : states) {
                    State p = m.get(s);
                    p.accept = s.accept;
                    if (s == initial)
                        a.initial = p;
                    p.transitions = new HashSet<>();
                    for (Transition t : s.transitions)
                        p.transitions.add(new Transition(t.min, t.max, m.get(t.to), t.taint));
                }
            }
            return a;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

class IntPair {

    int n1, n2;

    IntPair(int n1, int n2) {
        this.n1 = n1;
        this.n2 = n2;
    }
}

class StateList {

    int size;

    StateListNode first, last;

    StateListNode add(State q) {
        return new StateListNode(q, this);
    }
}

class StateListNode {

    State q;

    StateListNode next, prev;

    StateList sl;

    StateListNode(State q, StateList sl) {
        this.q = q;
        this.sl = sl;
        if (sl.size++ == 0)
            sl.first = sl.last = this;
        else {
            sl.last.next = this;
            prev = sl.last;
            sl.last = this;
        }
    }

    void remove() {
        sl.size--;
        if (sl.first == this)
            sl.first = next;
        else
            prev.next = next;
        if (sl.last == this)
            sl.last = prev;
        else
            next.prev = prev;
    }
}

class ShuffleConfiguration {

    ShuffleConfiguration prev;
    State[] ca_states;
    State a_state;
    char min;
    int hash;
    boolean shuffle_suspended;
    boolean surrogate;
    int suspended1;

    ShuffleConfiguration(Collection<Automaton> ca, Automaton a) {
        ca_states = new State[ca.size()];
        int i = 0;
        for (Automaton a1 : ca)
            ca_states[i++] = a1.getInitialState();
        a_state = a.getInitialState();
        computeHash();
    }

    ShuffleConfiguration(ShuffleConfiguration c, int i1, State s1, char min) {
        prev = c;
        ca_states = c.ca_states.clone();
        a_state = c.a_state;
        ca_states[i1] = s1;
        this.min = min;
        computeHash();
    }

    ShuffleConfiguration(ShuffleConfiguration c, int i1, State s1, State s2, char min) {
        prev = c;
        ca_states = c.ca_states.clone();
        a_state = c.a_state;
        ca_states[i1] = s1;
        a_state = s2;
        this.min = min;
        if (!surrogate) {
            shuffle_suspended = c.shuffle_suspended;
            suspended1 = c.suspended1;
        }
        computeHash();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ShuffleConfiguration) {
            ShuffleConfiguration c = (ShuffleConfiguration) obj;
            return shuffle_suspended == c.shuffle_suspended &&
                surrogate == c.surrogate &&
                suspended1 == c.suspended1 &&
                Arrays.equals(ca_states, c.ca_states) &&
                a_state == c.a_state;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private void computeHash() {
        int hash = 0;
        for (State ca_state : ca_states) hash ^= ca_state.hashCode();
        hash ^= a_state.hashCode() * 100;
        if (shuffle_suspended || surrogate)
            hash += suspended1;
    }
}