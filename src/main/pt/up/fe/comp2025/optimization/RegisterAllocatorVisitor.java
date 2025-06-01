package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterAllocatorVisitor {

    public static class InsufficientRegisterException extends RuntimeException {
        private final int resourcesRequired;

        public InsufficientRegisterException(int resourcesRequired) {
            super("Resource allocation failed: minimum required resources = " + resourcesRequired);
            this.resourcesRequired = resourcesRequired;
        }

        public int getRequiredResources() {
            return resourcesRequired;
        }
    }

    public List<Report> processMethodRegisters(OllirResult result, int availableRegisters) {
        List<Report> reportList = new ArrayList<>();
        ClassUnit programClass = result.getOllirClass();

        // Skip allocation if registers are not constrained
        if (availableRegisters == -1) {
            return reportList;
        }

        for (Method method : programClass.getMethods()) {
            // Skip constructor methods
            if (method.isConstructMethod()) {
                continue;
            }

            try {
                if (availableRegisters == 0) {
                    // Optimal allocation mode
                    findOptimalAllocation(method);
                    String allocationDetails = generateAllocationSummary(method);
                    reportList.add(Report.newLog(Stage.OPTIMIZATION, 0, 0,
                            "Method " + method.getMethodName() + " optimally allocated: " +
                                    allocationDetails, null));
                } else {
                    // Limited allocation mode
                    allocateWithinLimit(method, availableRegisters);
                    String allocationDetails = generateAllocationSummary(method);
                    reportList.add(Report.newLog(Stage.OPTIMIZATION, 0, 0,
                            "Method " + method.getMethodName() + " allocated with " +
                                    availableRegisters + " registers: " + allocationDetails, null));
                }
            } catch (InsufficientRegisterException e) {
                reportList.add(Report.newError(Stage.OPTIMIZATION, 0, 0,
                        "Method " + method.getMethodName() + " allocation failed: requires at least " +
                                e.getRequiredResources() + " registers", null));
                throw e;
            } catch (Exception e) {
                reportList.add(Report.newError(Stage.OPTIMIZATION, 0, 0,
                        "Error allocating registers for " + method.getMethodName() + ": " +
                                e.getMessage(), e));
                throw new RuntimeException(e);
            }
        }

        return reportList;
    }

    private void findOptimalAllocation(Method method) {
        DataFlowAnalyzer dataFlow = new DataFlowAnalyzer(method);
        dataFlow.analyzeVariableLiveness();

        InterferenceGraphBuilder graphBuilder = new InterferenceGraphBuilder(dataFlow);
        Map<String, Set<String>> interferences = graphBuilder.buildInterferenceGraph();
        Map<String, Set<String>> relatedVars = graphBuilder.getRelatedVariables();

        RegisterAssigner assigner = new RegisterAssigner(method, interferences, relatedVars);
        assigner.assignOptimalRegisters();
    }

    private void allocateWithinLimit(Method method, int limit) {
        DataFlowAnalyzer dataFlow = new DataFlowAnalyzer(method);
        dataFlow.analyzeVariableLiveness();

        InterferenceGraphBuilder graphBuilder = new InterferenceGraphBuilder(dataFlow);
        Map<String, Set<String>> Interferences = graphBuilder.buildInterferenceGraph();
        Map<String, Set<String>> relatedVars = graphBuilder.getRelatedVariables();

        RegisterAssigner assigner = new RegisterAssigner(method, Interferences, relatedVars);
        assigner.assignLimitedRegisters(limit);
    }

    private String generateAllocationSummary(Method method) {
        StringBuilder summary = new StringBuilder();
        Map<String, Descriptor> variables = method.getVarTable();

        List<String> varNames = new ArrayList<>(variables.keySet());
        Collections.sort(varNames);

        for (int i = 0; i < varNames.size(); i++) {
            String name = varNames.get(i);
            summary.append(name).append("→r")
                    .append(variables.get(name).getVirtualReg());

            if (i < varNames.size() - 1) {
                summary.append(", ");
            }
        }

        return summary.toString();
    }

    /**
     * Analyzes variable liveness across a method's instructions
     */
    private static class DataFlowAnalyzer {
        private final Method method;
        private final List<Instruction> instructionList;
        private final Map<Instruction, Set<String>> liveIn = new HashMap<>();
        private final Map<Instruction, Set<String>> liveOut = new HashMap<>();
        private final Map<Instruction, Set<String>> defined = new HashMap<>();
        private final Map<Instruction, Set<String>> used = new HashMap<>();
        private final Map<Instruction, List<Instruction>> nextInstructions = new HashMap<>();

        public DataFlowAnalyzer(Method method) {
            this.method = method;
            this.instructionList = method.getInstructions();
        }

        public Map<Instruction, Set<String>> getLiveOutMap() {
            return liveOut;
        }

        public void analyzeVariableLiveness() {
            setupDataFlowSets();
            buildInstructionGraph();
            identifyVarUsesAndDefs();
            calculateLiveness();
        }

        private void setupDataFlowSets() {
            for (Instruction inst : instructionList) {
                liveIn.put(inst, new HashSet<>());
                liveOut.put(inst, new HashSet<>());
                defined.put(inst, new HashSet<>());
                used.put(inst, new HashSet<>());
                nextInstructions.put(inst, new ArrayList<>());
            }
        }

        private void buildInstructionGraph() {
            method.buildCFG();

            for (int i = 0; i < instructionList.size(); i++) {
                Instruction current = instructionList.get(i);

                // Return instructions have no successors
                if (current instanceof ReturnInstruction) {
                    continue;
                }

                // Handle goto instructions
                if (current instanceof GotoInstruction gotoInst) {
                    Instruction targetInst = findInstructionByLabel(gotoInst.getLabel());
                    if (targetInst != null) {
                        nextInstructions.get(current).add(targetInst);
                    }
                    continue;
                }

                // Handle conditional branches
                if (current instanceof CondBranchInstruction condBranch) {
                    // Add branch target
                    Instruction targetInst = findInstructionByLabel(condBranch.getLabel());
                    if (targetInst != null) {
                        nextInstructions.get(current).add(targetInst);
                    }

                    // Add fall-through successor if not last instruction
                    if (i + 1 < instructionList.size()) {
                        nextInstructions.get(current).add(instructionList.get(i + 1));
                    }
                    continue;
                }

                // Normal flow - next instruction is successor
                if (i + 1 < instructionList.size()) {
                    nextInstructions.get(current).add(instructionList.get(i + 1));
                }
            }
        }

        private Instruction findInstructionByLabel(String label) {
            if (label == null) return null;

            HashMap<String, Instruction> labelMap = method.getLabels();
            return labelMap != null ? labelMap.get(label) : null;
        }

        private void identifyVarUsesAndDefs() {
            for (Instruction inst : instructionList) {
                analyzeInstruction(inst, defined.get(inst), used.get(inst));
            }
        }

        private void analyzeInstruction(Instruction inst, Set<String> defSet, Set<String> useSet) {
            if (inst instanceof AssignInstruction assign) {
                Element target = assign.getDest();

                // Handle destination (left-hand side)
                if (target instanceof Operand && !(target instanceof ArrayOperand)) {
                    String varName = ((Operand) target).getName();
                    if (!varName.equals("this")) {
                        defSet.add(varName);
                    }
                } else if (target instanceof ArrayOperand arrayTarget) {
                    useSet.add(arrayTarget.getName());
                    for (Element idx : arrayTarget.getIndexOperands()) {
                        captureUsedVariables(idx, useSet);
                    }
                }

                // Handle source (right-hand side)
                analyzeRightHandSide(assign.getRhs(), useSet);
            }
            else if (inst instanceof CallInstruction call) {
                if (call.getCaller() != null) {
                    captureUsedVariables(call.getCaller(), useSet);
                }

                for (Element arg : call.getArguments()) {
                    captureUsedVariables(arg, useSet);
                }
            }
            else if (inst instanceof ReturnInstruction ret && ret.hasReturnValue()) {
                captureUsedVariables(ret.getOperand().get(), useSet);
            }
            else if (inst instanceof CondBranchInstruction branch) {
                for (Element op : branch.getOperands()) {
                    captureUsedVariables(op, useSet);
                }
            }
            else if (inst instanceof PutFieldInstruction put) {
                for (Element op : put.getOperands()) {
                    captureUsedVariables(op, useSet);
                }
            }
            else if (inst instanceof GetFieldInstruction get) {
                for (Element op : get.getOperands()) {
                    captureUsedVariables(op, useSet);
                }
            }
            else if (inst instanceof UnaryOpInstruction unary) {
                captureUsedVariables(unary.getOperand(), useSet);
            }
            else if (inst instanceof BinaryOpInstruction binary) {
                captureUsedVariables(binary.getLeftOperand(), useSet);
                captureUsedVariables(binary.getRightOperand(), useSet);
            }
            else if (inst instanceof SingleOpInstruction single) {
                captureUsedVariables(single.getSingleOperand(), useSet);
            }

            // Remove 'this' from tracking sets as it's handled specially
            defSet.remove("this");
            useSet.remove("this");
        }

        private void analyzeRightHandSide(Instruction inst, Set<String> useSet) {
            if (inst instanceof SingleOpInstruction single) {
                captureUsedVariables(single.getSingleOperand(), useSet);
            }
            else if (inst instanceof BinaryOpInstruction binary) {
                captureUsedVariables(binary.getLeftOperand(), useSet);
                captureUsedVariables(binary.getRightOperand(), useSet);
            }
            else if (inst instanceof UnaryOpInstruction unary) {
                captureUsedVariables(unary.getOperand(), useSet);
            }
            else if (inst instanceof CallInstruction call) {
                if (call.getCaller() != null) {
                    captureUsedVariables(call.getCaller(), useSet);
                }

                for (Element arg : call.getArguments()) {
                    captureUsedVariables(arg, useSet);
                }
            }
            else if (inst instanceof GetFieldInstruction get) {
                for (Element op : get.getOperands()) {
                    captureUsedVariables(op, useSet);
                }
            }
        }

        private void captureUsedVariables(Element element, Set<String> useSet) {
            if (element instanceof Operand && !(element instanceof LiteralElement)) {
                if (element instanceof ArrayOperand arrayOp) {
                    useSet.add(arrayOp.getName());
                    for (Element idx : arrayOp.getIndexOperands()) {
                        captureUsedVariables(idx, useSet);
                    }
                } else {
                    String name = ((Operand) element).getName();
                    if (!name.equals("this")) {
                        useSet.add(name);
                    }
                }
            }
        }

        private void calculateLiveness() {
            boolean changed;

            do {
                changed = false;

                // Process instructions in reverse order for backward analysis
                for (int i = instructionList.size() - 1; i >= 0; i--) {
                    Instruction inst = instructionList.get(i);

                    // Save original sets for change detection
                    Set<String> prevIn = new HashSet<>(liveIn.get(inst));
                    Set<String> prevOut = new HashSet<>(liveOut.get(inst));

                    // OUT[n] = Union of IN[s] for all successors s
                    for (Instruction next : nextInstructions.get(inst)) {
                        liveOut.get(inst).addAll(liveIn.get(next));
                    }

                    // IN[n] = USE[n] U (OUT[n] - DEF[n])
                    Set<String> outExceptDef = new HashSet<>(liveOut.get(inst));
                    outExceptDef.removeAll(defined.get(inst));

                    Set<String> newIn = new HashSet<>(used.get(inst));
                    newIn.addAll(outExceptDef);

                    liveIn.get(inst).clear();
                    liveIn.get(inst).addAll(newIn);

                    // Check if sets changed
                    if (!prevIn.equals(liveIn.get(inst)) || !prevOut.equals(liveOut.get(inst))) {
                        changed = true;
                    }
                }
            } while (changed);
        }
    }

    /**
     * Builds the variable interference graph for register allocation
     */
    private static class InterferenceGraphBuilder {
        private final Map<String, Set<String>> interferenceGraph = new HashMap<>();
        private final Map<String, Set<String>> relatedVars = new HashMap<>();
        private final Map<Instruction, Set<String>> liveOutMap;
        private final Method method;

        public InterferenceGraphBuilder(DataFlowAnalyzer analyzer) {
            this.method = analyzer.method;
            this.liveOutMap = analyzer.getLiveOutMap();

            initializeGraphs();
            findRelatedVariables();
            buildInterferences();
            refineRelationships();
        }

        public Map<String, Set<String>> buildInterferenceGraph() {
            return interferenceGraph;
        }

        public Map<String, Set<String>> getRelatedVariables() {
            return relatedVars;
        }

        private void initializeGraphs() {
            for (String var : method.getVarTable().keySet()) {
                if (!var.equals("this")) {
                    interferenceGraph.put(var, new HashSet<>());
                    relatedVars.put(var, new HashSet<>());
                    relatedVars.get(var).add(var);  // Each var is related to itself
                }
            }
        }

        private void findRelatedVariables() {
            for (Instruction inst : method.getInstructions()) {
                if (!(inst instanceof AssignInstruction assign)) {
                    continue;
                }

                Element dest = assign.getDest();
                if (!(dest instanceof Operand) || dest instanceof ArrayOperand) {
                    continue;
                }

                String destVar = ((Operand) dest).getName();
                if (destVar.equals("this")) continue;

                // Case 1: Simple copy operation
                if (assign.getRhs() instanceof SingleOpInstruction sop) {
                    Element src = sop.getSingleOperand();
                    if (src instanceof Operand && !(src instanceof ArrayOperand)) {
                        String srcVar = ((Operand) src).getName();
                        if (!srcVar.equals("this")) {
                            relatedVars.get(destVar).add(srcVar);
                            relatedVars.get(srcVar).add(destVar);
                        }
                    }
                }

                // Case 2: Operations involving parameters and constants
                else if (assign.getRhs() instanceof BinaryOpInstruction bop) {
                    Element left = bop.getLeftOperand();
                    Element right = bop.getRightOperand();

                    boolean leftIsParameter = isMethodParameter(left);
                    boolean rightIsParameter = isMethodParameter(right);
                    boolean leftIsConstant = left instanceof LiteralElement;
                    boolean rightIsConstant = right instanceof LiteralElement;

                    // Parameter + constant operations often benefit from
                    // keeping the parameter and result in same register
                    if ((leftIsParameter && rightIsConstant) ||
                            (rightIsParameter && leftIsConstant)) {

                        String paramName = leftIsParameter ?
                                ((Operand)left).getName() : ((Operand)right).getName();

                        relatedVars.get(destVar).add(paramName);
                        relatedVars.get(paramName).add(destVar);
                    }
                }
            }

            propagateRelationships();
        }

        private boolean isMethodParameter(Element element) {
            if (!(element instanceof Operand) || element instanceof ArrayOperand) {
                return false;
            }

            String name = ((Operand)element).getName();
            if (name.equals("this")) return false;

            return method.getParams().stream()
                    .filter(Operand.class::isInstance)
                    .map(param -> ((Operand)param).getName())
                    .anyMatch(name::equals);
        }

        private void propagateRelationships() {
            boolean changed;
            do {
                changed = false;

                for (String var : relatedVars.keySet()) {
                    Set<String> newlyRelated = new HashSet<>();

                    // Transitive relationship: if A relates to B and B relates to C,
                    // then A relates to C
                    for (String related : relatedVars.get(var)) {
                        newlyRelated.addAll(relatedVars.get(related));
                    }

                    if (relatedVars.get(var).addAll(newlyRelated)) {
                        changed = true;
                    }
                }
            } while (changed);
        }

        private void buildInterferences() {
            Set<String> methodCallResults = new HashSet<>();

            for (Instruction inst : method.getInstructions()) {
                if (!(inst instanceof AssignInstruction assign)) continue;
                if (!(assign.getDest() instanceof Operand)) continue;

                String targetVar = ((Operand) assign.getDest()).getName();
                if (targetVar.equals("this")) continue;

                boolean isMethodCallResult = assign.getRhs() instanceof CallInstruction;
                if (isMethodCallResult) {
                    methodCallResults.add(targetVar);
                }

                String sourceVar = null;
                boolean isCopyOperation = false;

                // Check if this is a simple copy operation
                if (assign.getRhs() instanceof SingleOpInstruction sop &&
                        sop.getSingleOperand() instanceof Operand &&
                        !(sop.getSingleOperand() instanceof ArrayOperand)) {
                    sourceVar = ((Operand) sop.getSingleOperand()).getName();
                    isCopyOperation = true;
                }

                // Get variables live after this instruction
                Set<String> liveVariables = new HashSet<>(liveOutMap.get(inst));

                // For copy instructions, source not considered live afterward
                if (isCopyOperation && sourceVar != null && !sourceVar.equals("this")) {
                    liveVariables.remove(sourceVar);
                }

                // Target variable interferences with all other live variables
                for (String liveVar : liveVariables) {
                    if (!liveVar.equals(targetVar) && !liveVar.equals("this")) {
                        addInterference(targetVar, liveVar);
                    }
                }

                // Method call arguments need special handling for potential spills
                if (isMethodCallResult && assign.getRhs() instanceof CallInstruction call) {
                    for (Element arg : call.getArguments()) {
                        if (arg instanceof Operand && !(arg instanceof ArrayOperand)) {
                            String argName = ((Operand) arg).getName();
                            if (!argName.equals("this") && !argName.equals(targetVar)) {
                                addInterference(targetVar, argName);
                            }
                        }
                    }
                }
            }

            // Results of method calls must not interfere with each other
            List<String> resultsList = new ArrayList<>(methodCallResults);
            for (int i = 0; i < resultsList.size(); i++) {
                for (int j = i + 1; j < resultsList.size(); j++) {
                    addInterference(resultsList.get(i), resultsList.get(j));
                }
            }
        }

        private void refineRelationships() {
            for (String var : interferenceGraph.keySet()) {
                for (String related : new ArrayList<>(relatedVars.get(var))) {
                    if (!related.equals(var)) {
                        boolean simultaneouslyLive = false;

                        // Check if these variables are ever live at the same time
                        for (Instruction inst : method.getInstructions()) {
                            Set<String> livesAfterInst = liveOutMap.get(inst);
                            if (livesAfterInst.contains(var) && livesAfterInst.contains(related)) {
                                simultaneouslyLive = true;
                                break;
                            }
                        }

                        // If not simultaneously live, remove interference edge
                        if (!simultaneouslyLive) {
                            interferenceGraph.get(var).remove(related);
                            interferenceGraph.get(related).remove(var);
                        }
                    }
                }
            }
        }

        private void addInterference(String var1, String var2) {
            if (interferenceGraph.containsKey(var1) && interferenceGraph.containsKey(var2)) {
                interferenceGraph.get(var1).add(var2);
                interferenceGraph.get(var2).add(var1);
            }
        }
    }

    /**
     * Assigns registers to variables based on interference graph
     */
    private static class RegisterAssigner {
        private final Method method;
        private final Map<String, Set<String>> interferences;
        private final Map<String, Set<String>> related;

        public RegisterAssigner(Method method, Map<String, Set<String>> interferences,
                                Map<String, Set<String>> related) {
            this.method = method;
            this.interferences = interferences;
            this.related = related;
        }

        public void assignOptimalRegisters() {
            List<Set<String>> registerSets = createRegisterSets();

            Map<String, Integer> assignments = new HashMap<>();
            int regIndex = 0;

            for (Set<String> varSet : registerSets) {
                for (String var : varSet) {
                    assignments.put(var, regIndex);
                }
                regIndex++;
            }

            updateRegisterTable(assignments);
        }

        private List<Set<String>> createRegisterSets() {
            List<Set<String>> sets = new ArrayList<>();
            Set<String> processedVars = new HashSet<>();

            // Process variables grouped by relations first
            for (String var : interferences.keySet()) {
                if (processedVars.contains(var)) continue;

                Set<String> currentSet = new HashSet<>();
                currentSet.add(var);
                processedVars.add(var);

                // Add non-interfering related variables to same set
                for (String relatedVar : related.get(var)) {
                    if (!relatedVar.equals(var) && !processedVars.contains(relatedVar)) {
                        boolean canBeAdded = true;

                        for (String member : currentSet) {
                            if (interferences.get(member).contains(relatedVar)) {
                                canBeAdded = false;
                                break;
                            }
                        }

                        if (canBeAdded) {
                            currentSet.add(relatedVar);
                            processedVars.add(relatedVar);
                        }
                    }
                }

                sets.add(currentSet);
            }

            return sets;
        }

        public void assignLimitedRegisters(int maxRegisters) {
            List<Set<String>> registerSets = createRegisterSets();

            // If we have enough registers for all sets
            if (registerSets.size() <= maxRegisters) {
                Map<String, Integer> assignments = new HashMap<>();
                int regIndex = 0;

                for (Set<String> set : registerSets) {
                    for (String var : set) {
                        assignments.put(var, regIndex);
                    }
                    regIndex++;
                }

                updateRegisterTable(assignments);
                return;
            }

            // Otherwise, use graph coloring approach
            try {
                Map<String, Integer> coloring = colorInterferenceGraph(maxRegisters);
                updateRegisterTable(coloring);
            } catch (Exception e) {
                int minimumNeeded = determineMinimumRegisters();
                throw new InsufficientRegisterException(minimumNeeded);
            }
        }

        private Map<String, Integer> colorInterferenceGraph(int maxColors) {
            Map<String, Integer> colorMap = new HashMap<>();
            Set<String> coloredVars = new HashSet<>();

            // Sort by connectivity (fewer neighbors first for better coloring)
            List<String> sortedVars = new ArrayList<>(interferences.keySet());
            sortedVars.sort(Comparator.comparingInt(v -> interferences.get(v).size()));

            for (String var : sortedVars) {
                if (coloredVars.contains(var)) continue;

                // Find first available color
                Set<Integer> neighborColors = new HashSet<>();
                for (String neighbor : interferences.get(var)) {
                    if (colorMap.containsKey(neighbor)) {
                        neighborColors.add(colorMap.get(neighbor));
                    }
                }

                int selectedColor = 0;
                while (neighborColors.contains(selectedColor) && selectedColor < maxColors) {
                    selectedColor++;
                }

                if (selectedColor >= maxColors) {
                    throw new InsufficientRegisterException(maxColors + 1);
                }

                // Assign color to variable and related variables if possible
                colorMap.put(var, selectedColor);
                coloredVars.add(var);

                // Try same color for related variables (preference coloring)
                for (String relatedVar : related.get(var)) {
                    if (!relatedVar.equals(var) && !coloredVars.contains(relatedVar)) {
                        boolean canUseColor = true;

                        for (String neighbor : interferences.get(relatedVar)) {
                            Integer neighborColor = colorMap.get(neighbor);
                            if (neighborColor != null && neighborColor == selectedColor) {
                                canUseColor = false;
                                break;
                            }
                        }

                        if (canUseColor) {
                            colorMap.put(relatedVar, selectedColor);
                            coloredVars.add(relatedVar);
                        }
                    }
                }
            }

            return colorMap;
        }

        private void updateRegisterTable(Map<String, Integer> assignments) {
            // Collect parameter names
            List<Element> parameters = method.getParams();
            Set<String> paramNames = parameters.stream()
                    .filter(Operand.class::isInstance)
                    .map(param -> ((Operand) param).getName())
                    .collect(Collectors.toSet());

            int nextParamReg = 0;

            // Handle 'this' parameter for non-static methods
            if (!method.isStaticMethod() && method.getVarTable().containsKey("this")) {
                method.getVarTable().get("this").setVirtualReg(nextParamReg++);
            }

            // Assign registers to parameters first
            for (Element param : parameters) {
                if (param instanceof Operand paramOp) {
                    String paramName = paramOp.getName();
                    if (!paramName.equals("this")) {
                        method.getVarTable().get(paramName).setVirtualReg(nextParamReg++);
                    }
                }
            }

            // Start local variables after parameter registers
            int localRegBase = nextParamReg;

            // Assign registers to local variables
            for (Map.Entry<String, Integer> entry : assignments.entrySet()) {
                String varName = entry.getKey();

                if (!paramNames.contains(varName) && !varName.equals("this")) {
                    method.getVarTable().get(varName).setVirtualReg(localRegBase + entry.getValue());
                }
            }
        }

        private int determineMinimumRegisters() {
            try {
                // Try coloring with unlimited registers to find minimum
                Map<String, Integer> coloring = colorInterferenceGraph(Integer.MAX_VALUE);

                int highestColor = -1;
                for (int color : coloring.values()) {
                    highestColor = Math.max(highestColor, color);
                }

                return highestColor + 1; // Colors are 0-indexed
            } catch (Exception e) {
                // Fallback: estimate based on max degree
                int maxDegree = 0;
                for (Set<String> neighbors : interferences.values()) {
                    maxDegree = Math.max(maxDegree, neighbors.size());
                }
                return maxDegree + 1; // Safe estimate
            }
        }
    }
}