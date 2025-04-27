package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test variable lookup.
 */
public class SymbolTableTest {

    static JmmSemanticsResult getSemanticsResult(String filename) {
        return TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/" + filename));
    }

    static JmmSemanticsResult test(String filename, boolean fail) {
        var semantics = getSemanticsResult(filename);
        if (fail) {
            TestUtils.mustFail(semantics.getReports());
        } else {
            TestUtils.noErrors(semantics.getReports());
        }
        return semantics;
    }

    /**
     * Test if fields are not being accessed from static methods.
     */
    @Test
    public void NumImports() {
        var semantics = test("symboltable/Imports.jmm", false);
        assertEquals(2, semantics.getSymbolTable().getImports().size());
    }

    @Test
    public void ClassAndSuper() {
        var semantics = test("symboltable/Super.jmm", false);
        assertEquals("Super", semantics.getSymbolTable().getClassName());
        assertEquals("UltraSuper", semantics.getSymbolTable().getSuper());

    }

    // Verifica os tipos de retorno dos métodos, testando nomes e se são arrays
    @Test
    public void ReturnTypes() {
        var semantics = test("symboltable/ReturnTypes.jmm", false);
        var st = semantics.getSymbolTable();
        assertEquals("int", st.getReturnType("sum").getName());
        assertFalse(st.getReturnType("sum").isArray());
        assertEquals("boolean", st.getReturnType("check").getName());
    }

    @Test
    public void Fields() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var fields = semantics.getSymbolTable().getFields();
        assertEquals(3, fields.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;

        for (var f : fields) {
            switch (f.getType().getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    break;
                case "boolean":
                    checkBool++;
                    break;
                case "int":
                    checkInt++;
                    break;
            }
        }
        ;
        assertEquals("Field of type int", 1, checkInt);
        assertEquals("Field of type boolean", 1, checkBool);
        assertEquals("Field of type object", 1, checkObj);

    }

    // Garante que um campo do tipo array de inteiros é corretamente identificado.
    @Test
    public void ArrayField() {
        var semantics = test("symboltable/ArrayField.jmm", false);
        var fields = semantics.getSymbolTable().getFields();
        assertEquals(1, fields.size());
        assertEquals("int", fields.get(0).getType().getName());
        assertTrue(fields.get(0).getType().isArray());
    }

    // Testa o uso de tipos de objeto como campos, parâmetros e retornos.
    @Test
    public void ObjectTypes() {
        var semantics = test("symboltable/ObjectTypes.jmm", false);
        var st = semantics.getSymbolTable();
        var field = st.getFields().get(0);
        assertEquals("ObjectTypes", field.getType().getName());
        var param = st.getParameters("test").get(0);
        assertEquals("ObjectTypes", param.getType().getName());
        var ret = st.getReturnType("test");
        assertEquals("ObjectTypes", ret.getName());
    }

    @Test
    public void Methods() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(5, methods.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;
        var checkAll = 0;

        for (var m : methods) {
            var ret = st.getReturnType(m);
            var numParameters = st.getParameters(m).size();
            switch (ret.getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "boolean":
                    checkBool++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "int":
                    if (ret.isArray()) {
                        checkAll++;
                        assertEquals("Method " + m + " parameters", 3, numParameters);
                    } else {
                        checkInt++;
                        assertEquals("Method " + m + " parameters", 0, numParameters);
                    }
                    break;

            }
        }
        ;
        assertEquals("Method with return type int", 1, checkInt);
        assertEquals("Method with return type boolean", 1, checkBool);
        assertEquals("Method with return type object", 1, checkObj);
        assertEquals("Method with three arguments", 1, checkAll);


    }

    // Verifica se os parâmetros de um metodo são corretamente identificados e ordenados, verificando tipos e quantidade.
    @Test
    public void Parameters() {
        var semantics = test("symboltable/Parameters.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(1, methods.size());
        var parameters = st.getParameters(methods.get(0));
        assertEquals(3, parameters.size());
        assertEquals("Parameter 1", "int", parameters.get(0).getType().getName());
        assertEquals("Parameter 2", "boolean", parameters.get(1).getType().getName());
        assertEquals("Parameter 3", "Parameters", parameters.get(2).getType().getName());
    }

    // Assegura que métodos sem parâmetros nem variáveis locais são tratados corretamente.
    @Test
    public void EmptyMethod() {
        var semantics = test("symboltable/EmptyMethod.jmm", false);
        var st = semantics.getSymbolTable();
        assertEquals(0, st.getParameters("doNothing").size());
        assertEquals(0, st.getLocalVariables("doNothing").size());
    }


    @Test
    public void LocalVariables() {
        var semantics = test("symboltable/Locals.jmm", false);
        var st = semantics.getSymbolTable();
        var locals = st.getLocalVariables("foo");
        assertEquals(2, locals.size());
        assertEquals("int", locals.get(0).getType().getName());
        assertEquals("boolean", locals.get(1).getType().getName());
    }

    // Garante que variáveis locais em diferentes escopos são corretamente geridas;
    @Test
    public void Shadowing() {
        var semantics = test("symboltable/Shadowing.jmm", false);
        var st = semantics.getSymbolTable();
        var fields = st.getFields();
        var locals = st.getLocalVariables("foo");
        assertEquals("int", fields.get(0).getType().getName());
        assertEquals("boolean", locals.get(0).getType().getName());
    }

    // Testa a definição de métodos com argumentos variáveis (varargs)
    @Test
    public void VarargsMethod() {
        var semantics = test("symboltable/Varargs.jmm", false);
        var st = semantics.getSymbolTable();
        var params = st.getParameters("printAll");
        assertEquals(1, params.size());
        assertEquals("int", params.get(0).getType().getName());
        assertTrue(params.get(0).getType().isArray());
    }
}
