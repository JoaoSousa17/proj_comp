package pt.up.fe.comp.cp2;

import org.junit.Test;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.type.BuiltinKind;
import pt.up.fe.comp.CpUtils;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

public class OllirTest {

    static OllirResult getOllirResult(String filename) {
        return TestUtils.optimize(SpecsIo.getResource("pt/up/fe/comp/cp2/ollir/" + filename));
    }

    public void compileBasic(ClassUnit classUnit) {
        // Test name of the class and super
        assertEquals("Class name not what was expected", "CompileBasic", classUnit.getClassName());
        assertEquals("Super class name not what was expected", "Quicksort", classUnit.getSuperClass());

        // Test method 1
        Method method1 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("method1"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method1", method1);

        var retInst1 = method1.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method1", retInst1.isPresent());

        // Test method 2
        Method method2 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("method2"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method2'", method2);

        var retInst2 = method2.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method2", retInst2.isPresent());
    }

    public void compileBasicWithFields(ClassUnit classUnit) {
        // Test name of the class and super
        assertEquals("Class name not what was expected", "CompileBasic", classUnit.getClassName());
        assertEquals("Super class name not what was expected", "Quicksort", classUnit.getSuperClass());

        // Test fields
        assertEquals("Class should have two fields", 2, classUnit.getNumFields());
        var fieldNames = new HashSet<>(Arrays.asList("intField", "boolField"));
        assertThat(fieldNames, hasItem(classUnit.getField(0).getFieldName()));
        assertThat(fieldNames, hasItem(classUnit.getField(1).getFieldName()));

        // Test method 1
        Method method1 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("method1"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method1", method1);

        var retInst1 = method1.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method1", retInst1.isPresent());

        // Test method 2
        Method method2 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("method2"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method2'", method2);

        var retInst2 = method2.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method2", retInst2.isPresent());
    }

    public void compileArithmetic(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileArithmetic", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var binOpInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction)
                .map(instr -> (AssignInstruction) instr)
                .filter(assign -> assign.getRhs() instanceof BinaryOpInstruction)
                .findFirst();

        assertTrue("Could not find a binary op instruction in method " + methodName, binOpInst.isPresent());

        var retInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method " + methodName, retInst.isPresent());
    }

    public void compileMethodInvocation(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileMethodInvocation", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var callInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof CallInstruction)
                .map(CallInstruction.class::cast)
                .findFirst();
        assertTrue("Could not find a call instruction in method " + methodName, callInst.isPresent());

        assertEquals("Invocation type not what was expected", InvokeStaticInstruction.class,
                callInst.get().getClass());
    }

    public void compileAssignment(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileAssignment", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var assignInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction)
                .map(AssignInstruction.class::cast)
                .findFirst();
        assertTrue("Could not find an assign instruction in method " + methodName, assignInst.isPresent());

        assertEquals("Assignment does not have the expected type", BuiltinKind.INT32, CpUtils.toBuiltinKind(assignInst.get().getTypeOfAssign()));
    }


    @Test
    public void section1_Basic_Class() {
        var result = getOllirResult("basic/BasicClass.jmm");

        compileBasic(result.getOllirClass());
    }

    @Test
    public void section1_Basic_Class_With_Fields() {
        var result = getOllirResult("basic/BasicClassWithFields.jmm");

        compileBasic(result.getOllirClass());
    }

    @Test
    public void section1_Basic_Assignment() {
        var result = getOllirResult("basic/BasicAssignment.jmm");

        compileAssignment(result.getOllirClass());
    }

    @Test
    public void section1_Basic_Method_Invocation() {
        var result = getOllirResult("basic/BasicMethodInvocation.jmm");

        compileMethodInvocation(result.getOllirClass());
    }


    /*checks if method declaration is correct (array)*/
    @Test
    public void section1_Basic_Method_Declaration_Array() {
        var result = getOllirResult("basic/BasicMethodsArray.jmm");

        var method = CpUtils.getMethod(result, "func4");

        CpUtils.assertEquals("Method return type", "int[]", CpUtils.toString(method.getReturnType()), result);
    }

    @Test
    public void section2_Arithmetic_Simple_add() {
        var ollirResult = getOllirResult("arithmetic/Arithmetic_add.jmm");

        compileArithmetic(ollirResult.getOllirClass());
    }

    @Test
    public void section2_Arithmetic_Simple_and() {
        var ollirResult = getOllirResult("arithmetic/Arithmetic_and.jmm");

        var method = CpUtils.getMethod(ollirResult, "main");

        CpUtils.assertHasOperation(OperationType.ANDB, method, ollirResult);
    }

    @Test
    public void section2_Arithmetic_Simple_less() {
        var ollirResult = getOllirResult("arithmetic/Arithmetic_less.jmm");

        var method = CpUtils.getMethod(ollirResult, "main");

        CpUtils.assertHasOperation(OperationType.LTH, method, ollirResult);

    }

    @Test
    public void section3_ControlFlow_If_Simple_Single_goto() {

        var result = getOllirResult("control_flow/SimpleIfElseStat.jmm");

        var method = CpUtils.getMethod(result, "func");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertEquals("Number of branches", 1, branches.size(), result);

        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Has at least 1 goto", gotos.size() >= 1, result);
    }

    @Test
    public void section3_ControlFlow_If_Switch() {

        var result = getOllirResult("control_flow/SwitchStat.jmm");

        var method = CpUtils.getMethod(result, "func");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertEquals("Number of branches", 6, branches.size(), result);

        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Has at least 6 gotos", gotos.size() >= 6, result);
    }

    @Test
    public void section3_ControlFlow_While_Simple() {

        var result = getOllirResult("control_flow/SimpleWhileStat.jmm");

        var method = CpUtils.getMethod(result, "func");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);

        CpUtils.assertTrue("Number of branches between 1 and 2", branches.size() > 0 && branches.size() < 3, result);
    }


    /*checks if an array is correctly initialized*/
    @Test
    public void section4_Arrays_Init_Array() {
        var result = getOllirResult("arrays/ArrayInit.jmm");

        var method = CpUtils.getMethod(result, "main");

        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);

        CpUtils.assertEquals("Number of calls", 3, calls.size(), result);

        // Get new
        var newCalls = calls.stream().filter(call -> call instanceof NewInstruction)
                .collect(Collectors.toList());

        CpUtils.assertEquals("Number of 'new' calls", 1, newCalls.size(), result);

        // Get length
        var lengthCalls = calls.stream().filter(call -> call instanceof ArrayLengthInstruction)
                .collect(Collectors.toList());

        CpUtils.assertEquals("Number of 'arraylenght' calls", 1, lengthCalls.size(), result);
    }

    /*checks if the access to the elements of array is correct*/
    @Test
    public void section4_Arrays_Access_Array() {
        var result = getOllirResult("arrays/ArrayAccess.jmm");

        var method = CpUtils.getMethod(result, "foo");

        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);
        var numArrayStores = assigns.stream().filter(assign -> assign.getDest() instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array stores", 5, numArrayStores, result);

        var numArrayReads = assigns.stream()
                .flatMap(assign -> CpUtils.getElements(assign.getRhs()).stream())
                .filter(element -> element instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array reads", 5, numArrayReads, result);
    }

    /*checks multiple expressions as indexes to access the elements of an array*/
    @Test
    public void section4_Arrays_Load_ComplexArrayAccess() {
        // Just parse
        var result = getOllirResult("arrays/ComplexArrayAccess.jmm");

        System.out.println("---------------------- OLLIR ----------------------");
        System.out.println(result.getOllirCode());
        System.out.println("---------------------- OLLIR ----------------------");

        var method = CpUtils.getMethod(result, "main");

        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);
        var numArrayStores = assigns.stream().filter(assign -> assign.getDest() instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array stores", 5, numArrayStores, result);

        var numArrayReads = assigns.stream()
                .flatMap(assign -> CpUtils.getElements(assign.getRhs()).stream())
                .filter(element -> element instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array reads", 6, numArrayReads, result);
    }

    @Test
    public void testNestedExpressions() {
        var result = getOllirResult("complex/NestedExpressions.jmm");
        var method = CpUtils.getMethod(result, "compute");

        // Verify multiple binary operations are present
        CpUtils.assertHasOperation(OperationType.ADD, method, result);
        CpUtils.assertHasOperation(OperationType.MUL, method, result);
        CpUtils.assertHasOperation(OperationType.DIV, method, result);

        // Check that we have the right number of operations (should have at least 3)
        var ops = CpUtils.getInstructions(OpInstruction.class, method);
        CpUtils.assertTrue("Expected at least 3 operations in nested expression", ops.size() >= 3, result);
    }

    @Test
    public void testVarargsMethod() {
        var result = getOllirResult("arrays/VarargsMethod.jmm");
        var method = CpUtils.getMethod(result, "sum");

        // Check return instruction exists
        CpUtils.assertReturnExists(method, result);

        // Verify the method has the varargs keyword in OLLIR
        String ollirCode = result.getOllirCode();
        assertTrue("Method should be marked as varargs in OLLIR",
                ollirCode.contains("varargs") && ollirCode.contains("sum"));

        // Test the method invocation with multiple arguments
        var mainMethod = CpUtils.getMethod(result, "main");
        var calls = CpUtils.assertInstExists(CallInstruction.class, mainMethod, result);
        CpUtils.assertTrue("Expected at least one method call", calls.size() >= 1, result);
    }

    @Test
    public void testIfElseNesting() {
        var result = getOllirResult("control_flow/NestedIfElse.jmm");
        var method = CpUtils.getMethod(result, "test");

        // Verify multiple conditional branches
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected at least 2 conditional branches", branches.size() >= 2, result);

        // Check for goto instructions that should be part of the control flow
        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Expected at least 2 goto instructions", gotos.size() >= 2, result);
    }

    @Test
    public void testWhileLoopWithBreak() {
        var result = getOllirResult("control_flow/WhileWithBreak.jmm");
        var method = CpUtils.getMethod(result, "count");

        // Verify conditional branch for while loop
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected at least one conditional branch", branches.size() >= 1, result);

        // Check for gotos that would implement the break
        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Expected at least one goto instruction for break", gotos.size() >= 1, result);
    }

    @Test
    public void testArrayOperations() {
        var result = getOllirResult("arrays/ComplexArrayOps.jmm");
        var method = CpUtils.getMethod(result, "processArray");

        // Check array operations
        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);

        // Count array stores
        var arrayStores = assigns.stream()
                .filter(assign -> assign.getDest() instanceof ArrayOperand)
                .count();
        CpUtils.assertTrue("Expected at least 2 array store operations", arrayStores >= 2, result);

        // Check if there are array reads in expressions
        var arrayReadsInRhs = assigns.stream()
                .flatMap(assign -> CpUtils.getElements(assign.getRhs()).stream())
                .filter(element -> element instanceof ArrayOperand)
                .count();
        CpUtils.assertTrue("Expected at least 2 array read operations", arrayReadsInRhs >= 2, result);
    }

    @Test
    public void testMethodCallChaining() {
        var result = getOllirResult("methods/MethodChaining.jmm");
        var method = CpUtils.getMethod(result, "chainCalls");

        // Check for method invocations
        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);
        CpUtils.assertTrue("Expected at least 2 method calls for chaining", calls.size() >= 2, result);

        // Verify structure of chained calls - there should be temporary variables to store intermediate results
        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);
        CpUtils.assertTrue("Expected assignments for storing intermediate results", assigns.size() >= 2, result);
    }

    @Test
    public void testObjectCreationAndFields() {
        var result = getOllirResult("objects/ObjectCreation.jmm");
        var method = CpUtils.getMethod(result, "createAndUse");

        // Check for new object creation
        var newCalls = CpUtils.getInstructions(CallInstruction.class, method).stream()
                .filter(call -> call instanceof NewInstruction)
                .collect(Collectors.toList());
        CpUtils.assertTrue("Expected at least one new instruction", newCalls.size() >= 1, result);

        // Check field access
        var fieldOps = CpUtils.getInstructions(GetFieldInstruction.class, method);
        CpUtils.assertTrue("Expected at least one getfield instruction", fieldOps.size() >= 1, result);

        // Check field modification
        var fieldSets = CpUtils.getInstructions(PutFieldInstruction.class, method);
        CpUtils.assertTrue("Expected at least one putfield instruction", fieldSets.size() >= 1, result);
    }

    @Test
    public void testBooleanLogic() {
        var result = getOllirResult("logic/BooleanOperations.jmm");
        var method = CpUtils.getMethod(result, "logicalOperations");

        // Check for boolean operations
        CpUtils.assertHasOperation(OperationType.AND, method, result);
        CpUtils.assertHasOperation(OperationType.OR, method, result);

        // Should have condition branches for boolean logic
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected at least 2 conditional branches for boolean logic", branches.size() >= 2, result);
    }

    @Test
    public void testMultipleReturnPaths() {
        var result = getOllirResult("control_flow/MultipleReturns.jmm");
        var method = CpUtils.getMethod(result, "findMax");

        // Should have multiple return instructions
        var returns = method.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .count();
        CpUtils.assertTrue("Expected multiple return instructions", returns > 1, result);

        // Should have conditional branches for different return paths
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected conditional branches for return paths", branches.size() >= 1, result);
    }

    @Test
    public void testStringOperations() {
        var result = getOllirResult("strings/StringOps.jmm");
        var method = CpUtils.getMethod(result, "processString");

        // Check for method invocations that would handle strings
        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);
        CpUtils.assertTrue("Expected method calls for string operations", calls.size() >= 1, result);

        // Check that string literals appear in the code
        boolean foundStringLiteral = method.getInstructions().stream()
                .flatMap(inst -> CpUtils.getElements(inst).stream())
                .filter(el -> el instanceof LiteralElement)
                .map(el -> (LiteralElement) el)
                .anyMatch(lit -> lit.getLiteral().startsWith("\""));

        CpUtils.assertTrue("Expected to find string literals", foundStringLiteral, result);
    }

    @Test
    public void testComplexExpressions() {
        var result = getOllirResult("arithmetic/ComplexExpressions.jmm");
        var method = CpUtils.getMethod(result, "compute");

        // Verificar operações aritméticas
        CpUtils.assertHasOperation(OperationType.ADD, method, result);
        CpUtils.assertHasOperation(OperationType.MUL, method, result);
        CpUtils.assertHasOperation(OperationType.DIV, method, result);
        CpUtils.assertHasOperation(OperationType.SUB, method, result);

        // Verificar quantidade de operações
        CpUtils.assertNumberOfOperations(OperationType.ADD, 2, method, result);
        CpUtils.assertNumberOfOperations(OperationType.MUL, 2, method, result);

        // Verificar que a ordem das operações está correta (deve haver temporários para resultados intermediários)
        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter múltiplas instruções de atribuição para cálculos intermediários",
                assigns.size() >= 4, result);
    }

    @Test
    public void testMathFunctions() {
        var result = getOllirResult("arithmetic/MathFunctions.jmm");

        // Testar método pow
        var powMethod = CpUtils.getMethod(result, "pow");

        // Verificar loop while
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, powMethod, result);
        CpUtils.assertTrue("Deve ter uma instrução de branch para o loop", branches.size() >= 1, result);

        // Testar método abs
        var absMethod = CpUtils.getMethod(result, "abs");

        // Verificar condição if
        var absBranches = CpUtils.assertInstExists(CondBranchInstruction.class, absMethod, result);
        CpUtils.assertTrue("Deve ter uma instrução de branch", absBranches.size() >= 1, result);

        // Verificar operação de negação
        var absAssigns = CpUtils.assertInstExists(AssignInstruction.class, absMethod, result);
        boolean hasNegation = false;
        for (var assign : absAssigns) {
            if (assign.getRhs() instanceof OpInstruction) {
                OpInstruction op = (OpInstruction) assign.getRhs();
                // Verificar se a operação é uma subtração onde o primeiro operando é 0, simulando uma negação
                if (op.getOperation().getOpType() == OperationType.SUB) {
                    // Verificar se parece uma negação (ex: 0 - x)
                    var operands = op.getOperands();
                    if (operands.size() == 2 && operands.get(0) instanceof LiteralElement) {
                        LiteralElement firstOp = (LiteralElement) operands.get(0);
                        if (firstOp.getLiteral().equals("0")) {
                            hasNegation = true;
                            break;
                        }
                    }
                }
            }
        }

        CpUtils.assertTrue("Deve ter uma operação de negação", hasNegation, result);
    }

    @Test
    public void testMultiDimensionalArrays() {
        var result = getOllirResult("arrays/MultiDimensional.jmm");
        var method = CpUtils.getMethod(result, "sumMatrix");

        // Verificar loops aninhados
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 instruções de branch para loops aninhados", branches.size() >= 2, result);

        // Verificar acesso a array bidimensional
        var instructions = method.getInstructions();

        boolean hasNestedArrayAccess = false;
        for (var inst : instructions) {
            if (inst instanceof AssignInstruction) {
                var rhs = ((AssignInstruction) inst).getRhs();
                if (rhs instanceof SingleOpInstruction) {
                    var operand = ((SingleOpInstruction) rhs).getSingleOperand();
                    if (operand instanceof ArrayOperand) {
                        // Verificar se estamos acessando um array que pode ser bidimensional
                        hasNestedArrayAccess = true;
                        break;
                    }
                }
            }
        }

        CpUtils.assertTrue("Deve acessar elementos de arrays", hasNestedArrayAccess, result);

        // Testar método de criação de matriz
        var createMethod = CpUtils.getMethod(result, "createMatrix");

        // Verificar que existem instâncias de new para arrays
        var newInsts = CpUtils.getInstructions(NewInstruction.class, createMethod);
        CpUtils.assertTrue("Deve criar novos arrays para a matriz", newInsts.size() >= 2, result);
    }

    @Test
    public void testSortingAlgorithm() {
        var result = getOllirResult("arrays/Sorting.jmm");
        var method = CpUtils.getMethod(result, "sort");

        // Verificar loops aninhados
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 instruções de branch para loops aninhados", branches.size() >= 2, result);

        // Verificar operações de acesso e modificação de array
        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);

        // Verificar arrays no lado esquerdo de atribuições (stores)
        int arrayStores = 0;
        for (var assign : assigns) {
            if (assign.getDest() instanceof ArrayOperand) {
                arrayStores++;
            }
        }
        CpUtils.assertTrue("Deve ter pelo menos 2 operações de escrita em array", arrayStores >= 2, result);

        // Verificar arrays no lado direito de atribuições (loads)
        int arrayReads = 0;
        for (var assign : assigns) {
            var elements = CpUtils.getElements(assign.getRhs());
            for (var element : elements) {
                if (element instanceof ArrayOperand) {
                    arrayReads++;
                }
            }
        }
        CpUtils.assertTrue("Deve ter múltiplas operações de leitura de array", arrayReads >= 4, result);
    }

    @Test
    public void testInheritance() {
        var result = getOllirResult("basic/Inheritance.jmm");
        var method = CpUtils.getMethod(result, "callBaseMethod");

        // Verificar chamadas a métodos
        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 chamadas de método", calls.size() >= 2, result);

        // Verificar que existe uma chamada ao método da superclasse
        boolean hasSuperCall = false;
        for (var call : calls) {
            if (call instanceof CallInstruction) {
                // Verificar se é uma invocação a um método da superclasse
                if (call instanceof InvokeSpecialInstruction || call instanceof InvokeStaticInstruction) {
                    hasSuperCall = true;
                    break;
                }
            }
        }

        CpUtils.assertTrue("Deve ter uma chamada a um método da superclasse", hasSuperCall, result);

        // Verificar que existe uma chamada ao método sobrescrito
        boolean hasOverrideCall = false;
        for (var call : calls) {
            if (call instanceof CallInstruction) {
                if (call.getMethodName() instanceof LiteralElement) {
                    LiteralElement lit = (LiteralElement) call.getMethodName();
                    if (lit.getLiteral().equals("overriddenMethod")) {
                        hasOverrideCall = true;
                        break;
                    }
                }
            }
        }

        CpUtils.assertTrue("Deve ter uma chamada ao método sobrescrito", hasOverrideCall, result);
    }

    @Test
    public void testFieldOperations() {
        var result = getOllirResult("basic/FieldOperations.jmm");

        // Testar inicialização de campos
        var initMethod = CpUtils.getMethod(result, "initializeFields");
        var putFields = CpUtils.getInstructions(PutFieldInstruction.class, initMethod);
        CpUtils.assertTrue("Deve ter pelo menos 3 atribuições a campos", putFields.size() >= 3, result);

        // Testar acesso a campos
        var getMethod = CpUtils.getMethod(result, "getSum");
        var getFields = CpUtils.getInstructions(GetFieldInstruction.class, getMethod);
        CpUtils.assertTrue("Deve acessar campos", getFields.size() >= 2, result);

        // Verificar decisão condicional baseada em campo
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, getMethod, result);
        CpUtils.assertTrue("Deve ter um branch baseado no campo flag", branches.size() >= 1, result);
    }

    @Test
    public void testNestedLoops() {
        var result = getOllirResult("control_flow/NestedLoops.jmm");
        var method = CpUtils.getMethod(result, "multiplyTable");

        // Verificar loops aninhados
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 instruções de branch para loops aninhados", branches.size() >= 2, result);

        // Verificar gotos
        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 instruções goto para loops", gotos.size() >= 2, result);

        // Verificar operação de multiplicação
        CpUtils.assertHasOperation(OperationType.MUL, method, result);
    }

    @Test
    public void testComplexControlFlow() {
        var result = getOllirResult("control_flow/Complex.jmm");
        var method = CpUtils.getMethod(result, "processWithBreaks");

        // Verificar múltiplos branches
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 3 instruções de branch", branches.size() >= 3, result);

        // Verificar gotos
        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 instruções goto", gotos.size() >= 2, result);

        // Verificar operações módulo (operações aritméticas para calcular o resto)
        var opInsts = CpUtils.getInstructions(OpInstruction.class, method);
        boolean hasModuloOps = false;

        for (var op : opInsts) {
            if (op.getOperation().getOpType() == OperationType.REM ||
                    op.getOperation().getOpType() == OperationType.DIV) {
                hasModuloOps = true;
                break;
            }
        }

        CpUtils.assertTrue("Deve ter operações para cálculo de módulo", hasModuloOps, result);
    }

    @Test
    public void testRecursiveFactorial() {
        var result = getOllirResult("control_flow/RecursiveFactorial.jmm");
        var method = CpUtils.getMethod(result, "compute");

        // Verificar chamada recursiva
        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);

        boolean hasRecursiveCall = false;
        for (var call : calls) {
            if (call.getMethodName() instanceof LiteralElement) {
                LiteralElement lit = (LiteralElement) call.getMethodName();
                if (lit.getLiteral().equals("compute")) {
                    hasRecursiveCall = true;
                    break;
                }
            }
        }

        CpUtils.assertTrue("Deve ter uma chamada recursiva", hasRecursiveCall, result);

        // Verificar condição de parada
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter uma condição de parada", branches.size() >= 1, result);

        // Verificar multiplicação
        CpUtils.assertHasOperation(OperationType.MUL, method, result);
    }

    @Test
    public void testBinarySearch() {
        var result = getOllirResult("control_flow/BinarySearch.jmm");
        var method = CpUtils.getMethod(result, "search");

        // Verificar chamadas recursivas
        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);

        int recursiveCalls = 0;
        for (var call : calls) {
            if (call.getMethodName() instanceof LiteralElement) {
                LiteralElement lit = (LiteralElement) call.getMethodName();
                if (lit.getLiteral().equals("search")) {
                    recursiveCalls++;
                }
            }
        }

        CpUtils.assertTrue("Deve ter pelo menos 2 chamadas recursivas (para cada ramo)", recursiveCalls >= 2, result);

        // Verificar acesso a array
        boolean hasArrayAccess = false;
        for (var inst : method.getInstructions()) {
            for (var element : CpUtils.getElements(inst)) {
                if (element instanceof ArrayOperand) {
                    hasArrayAccess = true;
                    break;
                }
            }
            if (hasArrayAccess) break;
        }

        CpUtils.assertTrue("Deve acessar o array pelo menos uma vez", hasArrayAccess, result);

        // Verificar múltiplos branches
        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Deve ter pelo menos 2 instruções de branch para as comparações", branches.size() >= 2, result);
    }

}
