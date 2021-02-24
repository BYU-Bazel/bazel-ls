package server.bazel.interp;

import org.junit.Assert;
import org.junit.Test;

public class LabelTest {
    @Test
    public void test_parse_withNoWorkspace() throws LabelSyntaxException {
        String value = "//path/to:target";
        Label l = Label.parse(value);

        Assert.assertFalse(l.hasWorkspace());
        Assert.assertEquals(LabelPkg.fromString("path/to"), l.pkg());
        Assert.assertEquals(LabelName.fromString("target"), l.name());
        Assert.assertFalse(l.isLocal());
        Assert.assertFalse(l.isSourceFile());
    }

    @Test
    public void test_parse_localDependency() throws LabelSyntaxException {
        String value = ":something";
        Label l = Label.parse(value);

        Assert.assertFalse(l.hasWorkspace());
        Assert.assertFalse(l.hasPkg());
        Assert.assertEquals(LabelName.fromString("something"), l.name());
        Assert.assertTrue(l.isLocal());
        Assert.assertFalse(l.isSourceFile());
    }

    @Test
    public void test_parse_failsWithTrailingSlash() {
        try {
            String value = "//path/to/:invalid";
            Label.parse(value);
            Assert.fail();
        } catch (LabelSyntaxException e) {
            // Will only get here if failed to parse.
        }
    }

    @Test
    public void test_parse_failsWhenGivenEmptyValue() {
        try {
            String value = "";
            Label.parse(value);
            Assert.fail();
        } catch (LabelSyntaxException ls) {
            // Will only get here if failed to parse.
        }
    }

    @Test
    public void test_parse_impliedPkgAndName() throws LabelSyntaxException {
        String value = "@foo";
        Label l = Label.parse(value);

        Assert.assertEquals(LabelWorkspace.fromString("foo"), l.workspace());
        Assert.assertFalse(l.hasPkg());
        Assert.assertFalse(l.hasName());
        Assert.assertFalse(l.isLocal());
        Assert.assertFalse(l.isSourceFile());
    }

    @Test
    public void test_parse_sourceFileWithExtension() throws LabelSyntaxException {
        String value = "hello_world.cc";
        Label l = Label.parse(value);

        Assert.assertFalse(l.hasWorkspace());
        Assert.assertEquals(LabelPkg.fromString("hello_world.cc"), l.pkg());
        Assert.assertFalse(l.hasName());
        Assert.assertTrue(l.isSourceFile());
        Assert.assertFalse(l.isLocal());
    }

    @Test
    public void test_parse_sourceFileWithoutExtension() throws LabelSyntaxException {
        String value = "hello";
        Label l = Label.parse(value);
        Assert.assertFalse(l.hasWorkspace());
        Assert.assertEquals(LabelPkg.fromString("hello"), l.pkg());
        Assert.assertFalse(l.hasName());
        Assert.assertTrue(l.isSourceFile());
        Assert.assertFalse(l.isLocal());
    }
}
