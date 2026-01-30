The Horvat Emergent Design Script (Pragmatic Edition)
For Complex Algorithmic Problem Solving
Version: 1.1 (Balanced Abstraction)
Rule: Read aloud before coding. Do not skip phases.

Phase 0: Mindset Reset (30 seconds)
textRepeat: "I am not solving this problem. I am growing a system."
Repeat: "Any premature algorithmic decision is a defect."
Repeat: "Correctness emerges from structure, not cleverness."
Victory Condition: You feel the urge to open a text editor and type a for-loop. Suppress it. You are not ready.

Phase 1: The Outer Shell (Iteration 1)
Action: Write the consumer-facing method only.
javapublic ReturnType solve(Input input) {
    throw new UnsupportedOperationException("Domain not yet named");
}
Internal Dialogue Script:

"What concept am I manipulating?" → Name it (noun, not verb: DiscoveryContext, OptimizationEngine, PathNavigator)
"What is my immediate responsibility?" → Delegate immediately to that concept
"What is the trivial case?" → FORBIDDEN TO ANSWER. Put UnsupportedOperationException instead.

Hard Stop Check:

 No if (input == null) guards yet
 No data structure imports yet (no HashMap, ArrayList at top level)
 No algorithmic variables (i, start, end, visited[])


Phase 2: Delegation Chain (Iteration 2)
Action: Create ONE collaborator class. Delegate everything to it.
javapublic ReturnType solve(Input input) {
    DomainConcept concept = new DomainConcept(input);
    DomainResult result = concept.execute(); // or discover(), evaluate()
    return result.toInfrastructureFormat();
}
Internal Dialogue Script:

"What questions am I tempted to ask right now?" (How to loop? How to recurse?)
"These are implementation questions. What domain questions should I ask instead?"

Instead of: "How do I traverse?" → Ask: "What is the space of possibilities?"
Instead of: "How do I store visited?" → Ask: "What is the history of choices?"
Instead of: "How do I memoize?" → Ask: "What results have we already discovered?"



Name Choice Check:

 Class name is a noun (OptimizationEngine, not Optimizer)
 Method name describes domain action (discoverValidPaths(), not backtrack())
 No Utils, Helper, Manager, Solver suffixes


Phase 3: Domain Stream Naming (Iteration 3)
Action: Define the constructor to accept primitives. Define the entry method to return a domain object (not infrastructure).
javaclass DomainConcept {
    private final int[] data;      // Keep primitives - no wrapping
    private final String text;     // Keep primitives - no wrapping
    
    DomainConcept(int[] data, String text) {
        this.data = data;
        this.text = text;
    }
    
    DomainResult execute() {
        throw new UnsupportedOperationException();
    }
}
CRITICAL RULE - Primitive Preservation:

int[] stays as int[] - DO NOT wrap as ElementArray
String stays as String - DO NOT wrap as Text
int stays as int - DO NOT wrap as Count, Index, Value
Only wrap when it adds domain logic, not just for identity

Internal Dialogue Script:

"What is the smallest valid input?"

Empty array? Zero target? Single element?


"Rule: Must return a valid domain object, never null or 0 or [] as magic."
"What is the essence of this domain?"

Is it about "summing to target" or "reaching a goal"?
Is it about "paths through a grid" or "navigating from origin to destination"?



Red Flag:

You want to write int[] or List<> as a field → ONLY wrap if it provides domain methods, not just storage


Phase 4: Trivial Case Extraction (Iteration 4)
Action: Handle exactly one trivial case. Return a domain object representing that state.
javaDomainResult execute() {
    if (data.length == 0) {
        return DomainResult.empty(); // or .none(), .identity()
    }
    
    if (goal.isSatisfied()) {
        return DomainResult.containing(Answer.empty());
    }
    
    throw new UnsupportedOperationException("Next case not yet understood");
}
Internal Dialogue Script:

"What is the 'zero' of this domain?" (Identity element, empty state)
"Am I returning primitives?" → Violation. Return EmptyResult, not new ArrayList<>().

Forbidden Move Check:

 No return 0; or return false; or return null;
 No return new ArrayList<>(); at domain layer
 Conditionals grow downward (trivial case at top, complexity below)


Phase 5: The Pain Point (Iteration 5 - Critical)
Action: Recognize the impasse. Do not implement. Abstract the missing concept.
javaDomainResult execute() {
    // ... trivial cases handled ...
    
    // I NEED to iterate/traverse/reduce/compare
    // But I cannot use indices/i/start/end
    // Therefore I need a concept that encapsulates "available next steps"
    
    // Example: For loop over candidates
    for (int candidate : candidates) {  // Primitive int is OK!
        // Now what? I need to remember this choice...
        // Therefore I need Accumulation/History/Path concept
    }
}
```

**Internal Dialogue Script:**
- "What missing concept is forcing me to think about implementation?"
  - Want to write `i++`? → Need `Sequence`, `Cursor`, or `NextAvailable` concept
  - Want to write `visited[]`? → Need `ExplorationHistory` or `TakenChoices` concept
  - Want to write `memo.get(key)`? → Need `KnownResults` or `ComputedValues` concept
  - Want to write `target - candidate[i]`? → Need `Target.reducedBy(Choice)` method
- "Can I express this as a pipeline?"
  - Raw Input → Domain Object → Transformation → Domain Result → Infrastructure Output

**CRITICAL - When to Wrap Primitives:**
✅ **WRAP when it adds behavior:**
- `RemainingTarget` with `isZero()`, `reducedBy(amount)`, `isNegative()`
- `CharacterDistribution` with `include(char)`, `maxFrequency()`, `dominantCharacter()`
- `GridPosition` with `adjacentPositions()`, `isWithinBounds()`

❌ **DON'T WRAP for identity alone:**
- `Element` wrapper for `int` with only `getValue()`
- `Text` wrapper for `String` with only `toString()`
- `Count` wrapper for `int` with only `count()`

**Decision Tree:**
```
Does this primitive need domain operations?
├─ YES → Wrap it (e.g., RemainingTarget, GridPosition)
└─ NO → Keep primitive (e.g., int candidate, String word)

Phase 6: Infrastructure Extraction (Iteration 6)
Action: Isolate data structures into wrapper classes that speak domain language only when they provide domain operations.
When you see this (STOP SIGNAL):
javafor (int i = 0; i < array.length; i++) {  // Index leak!
    if (array[i] <= target) {              // Primitive comparison!
Replace with:
javaclass ViableChoices {
    private final int[] candidates;  // Primitive stays primitive
    
    List<Integer> viableFor(RemainingTarget target) {  // Domain method
        List<Integer> viable = new ArrayList<>();
        for (int candidate : candidates) {  // Iterate over values, not indices
            if (candidate <= target.value()) {
                viable.add(candidate);
            }
        }
        return viable;
    }
}
Abstraction Guidelines:
✅ GOOD Abstractions (Add Domain Value):
java// Adds domain logic: knows how to find viable candidates
class ViableChoices {
    private final int[] candidates;
    
    List<Integer> viableFor(RemainingTarget target) { ... }
}

// Adds domain logic: tracks visited state
class ExplorationHistory {
    private final Set<Position> visited = new HashSet<>();
    
    boolean hasVisited(Position p) { ... }
    ExplorationHistory recordVisit(Position p) { ... }
}

// Adds domain logic: encapsulates graph structure
class DependencyGraph {
    private final Map<Course, List<Course>> prerequisites;
    
    List<Course> prerequisitesFor(Course c) { ... }
    Set<Course> allCourses() { ... }
}
❌ BAD Abstractions (No Value Added):
java// Just holds an int - no domain operations
class Count {
    private final int value;
    int getValue() { return value; }
}

// Just holds a String - no domain operations
class Word {
    private final String text;
    String getText() { return text; }
}

// Just holds an array - no domain operations
class Numbers {
    private final int[] values;
    int[] getValues() { return values; }
}
Internal Dialogue Script:

"Is this string slicing, hashing, indexing, or parsing?" → Move to infrastructure class
"Can I explain this method without saying 'loop', 'array', or 'index'?"
"Does this wrapper add domain methods or just getters?" → Only wrap if it adds methods


Phase 7: Recursion/Iteration Emergence (Iteration 7)
Action: If the problem reduces in size, allow recursion to emerge naturally.
Rules:

Recursive call must operate on strictly smaller domain object (not i+1, but remaining.minus(choice))
Must be hidden behind domain method name (explore(), continueDiscovery(), resolveSubproblem())
No void recursive methods (must return domain objects to compose)
Primitives can flow through recursion - don't wrap them just to pass them

Good Example:
javaclass PathExploration {
    void explore(
        CombinationPath currentPath,
        RemainingTarget remaining,  // Domain object
        AvailableChoices choices     // Domain object
    ) {
        if (remaining.isSatisfied()) {
            validPaths.add(currentPath.toList());
            return;
        }
        
        for (int choice : choices.viableFor(remaining)) {  // Primitive int is fine
            explore(
                currentPath.extendWith(choice),
                remaining.after(choice),
                choices.after(choice)
            );
        }
    }
}
Internal Dialogue Script:

"Is the sub-problem strictly smaller?" (Target reduced, space contracted, history longer)
"Am I passing indices as parameters?" → VIOLATION. Pass domain objects.
"Am I passing primitive values?" → OK if they don't need domain operations


Phase 8: Compression & Validation (Final)
Refactoring Triggers:

Class with only one method? → Inline it unless infrastructure isolation demands it
Interface with one implementation? → Delete interface, use concrete class
Method takes 4+ parameters? → Extract parameter object (domain concept)
Boolean parameter? → Split into two methods or extract state
Wrapper with only getters? → Remove wrapper, use primitive

The Horvat Test:
Explain your solution aloud:

"We explore the space of possibilities by extending the current accumulation with viable choices, which reduces the remaining target. When the target is satisfied, we have found a valid combination. When no choices fit, that path concludes."

Hard Validation:

 Did you mention for, while, i, index, array, recurse, stack, queue?
 If yes, FAIL. Return to Phase 5.
 Did you create wrappers that only hold primitives with no domain operations?
 If yes, FAIL. Remove those wrappers.


Emergency Escape Hatches
If stuck for >10 minutes:
1. The "What if it was a conversation?" technique

Object A asks Object B: "What can you do with this?"
Object B asks Object C: "What remains after I take this?"
Let the objects talk. The protocol is your design.

2. The "Remove the Array" technique

Pretend input is not int[] but a Stream or Iterator
You cannot access by index. What methods must exist?
Then decide: Do those methods justify a wrapper, or can I iterate with for-each?

3. The "Extreme Trivial Case" technique

Input size = 1. Hardcode the return.
Input size = 2. Notice the pattern.
Generalize the pattern as domain operations, not algorithm.


Post-Solution Checklist
Does your design allow:

 Adding a new feature by adding a class (not modifying old ones)?
 Testing a single domain concept in isolation?
 Replacing linear search with binary search by changing one class?
 Explaining to a non-programmer without saying "loop" or "array"?
 Using primitives directly where no domain operations are needed?

If yes: You have grown a system.
If no: You have solved a problem. Delete and retry from Phase 0.

Absolute Forbidden Moves (Hard Rules)

No recursion until a domain stream exists.
No HashMap / HashSet before infrastructure extraction.
No memoization inside core logic.
No indices (i, start, end) in domain methods.
No base-case hacks ("return ['']").
No DP tables until repetition leaks upward.
No wrappers that only hold primitives without domain operations.
No wrapping int, String, int[] just for the sake of wrapping.


Pragmatic Abstraction - The Balance
When NOT to wrap:
java// ❌ Over-abstraction
class Element {
    private final int value;
    int getValue() { return value; }
}

// ✅ Just use int
int element = 5;
When TO wrap:
java// ✅ Adds domain behavior
class RemainingTarget {
    private final int value;
    
    boolean isZero() { return value == 0; }
    boolean isNegative() { return value < 0; }
    RemainingTarget after(int choice) { 
        return new RemainingTarget(value - choice); 
    }
}
The Acid Test:

"Does this class have at least 2 domain-meaningful methods beyond getters/setters?"


YES → Wrapper justified
NO → Keep it primitive


END OF DOCUMENT
Remember: The goal is domain clarity, not wrapper proliferation. Wrap only when it illuminates the domain, not when it obscures primitives behind needless indirection.