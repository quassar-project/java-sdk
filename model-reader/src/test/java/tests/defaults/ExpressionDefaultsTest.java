package tests.defaults;

import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionDefaultsTest {
    @Test void functionDefaultRetainsSpacesOperatorsAndStrings() throws Exception {
        String code = "self.count > 0 ? \"as private\" : \"empty\"";
        assertEquals(new Value.Expression(code), DefaultsContract.settings("\tdef function:Action action = '" + code + "' as private\n", "").get("first").property("action"));
    }
    @Test void explicitExpressionReplacesDefaultExpression() throws Exception {
        assertEquals(new Value.Expression("other.run(1, 2)"), DefaultsContract.settings("\tdef function:Action action = 'self.run()'\n", "\taction = 'other.run(1, 2)'\n").get("first").property("action"));
    }
    @Test void objectExpressionIsPreservedWithoutExecutingIt() throws Exception {
        assertEquals(new Value.Expression("new Object()"), DefaultsContract.settings("\tdef object:java.lang.Object data = 'new Object()'\n", "").get("first").property("data"));
    }
    @Test void concreteWordCanOverrideAnExpressionDefault() throws Exception {
        assertEquals(new Value.Reference("Closed"), DefaultsContract.settings("\tdef word:{Open Closed} state = 'chooseState()'\n", "\tstate = Closed\n").get("first").property("state"));
    }
    @Test void quotedAnnotationsAndCommentsRemainStringContent() throws Exception {
        assertEquals(new Value.Text("x as private // keep"), DefaultsContract.settings("\tdef string text = \"x as private // keep\" as private\n", "").get("first").property("text"));
    }
}
