/*
 * Copyright 2008 CoreMedia AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */

package net.jangaroo.test.integration;

import java.io.File;

/**
 * Some basic test cases for JangarooScript compiler and runtime correctness.
 *
 * @author Andreas Gawecki
 */

public class JooTest extends JooRuntimeTestCase {

  public JooTest(String name) {
    super(name);
  }


  public void testIdentityMethod() throws Exception {
    loadClass("package1.TestMethodCall");
    complete();
    initClass("package1.TestMethodCall");
    expectNumber(43, "package1.TestMethodCall.s(43)");
    eval("obj = new package1.TestMethodCall();");
    expectNumber(43, "obj.m(43)");
  }

  public void testAssert() throws Exception {
    String qualifiedName = "package1.TestAssert";
    String asFileName = asFileName(qualifiedName);
    String jsFileName = jsFileName(qualifiedName);
    loadClass(qualifiedName);
    complete();

    String canonicalJsFileName = new File(jsFileName).getCanonicalPath();
    // TODO: re-enable when we again have both kinds of tests:
    boolean assertionsEnabled = Boolean.valueOf("true"); //canonicalJsFileName.contains(File.separatorChar + "debug-and-assert" + File.separatorChar);

    final String script = qualifiedName + ".testAssert()";
    System.out.println("\ncanonicalJsFileName: " + canonicalJsFileName);
    System.out.println("\nassertions enabled: " + assertionsEnabled);

    if (!assertionsEnabled) {
      expectString("no exception thrown", script);
    } else {
      int line = 30;
      int column = 9;
      String expectedErrorMsgTail = asFileName + "(" + line + ":" + column + "): assertion failed";
      expectSubstring(expectedErrorMsgTail, qualifiedName + ".testAssert()");
    }
  }

  public void testInheritance() throws Exception {
    loadClass("package1.TestInheritanceSuperClass");
    loadClass("package1.TestInheritanceSubClass");
    loadClass("package1.TestInheritanceSubSubClass");
    complete();
    eval("obj1 = new package1.TestInheritanceSuperClass(1);");
    eval("obj2 = new package1.TestInheritanceSubClass(11, 2);");
    eval("obj3 = new package1.TestInheritanceSubSubClass(111, 22, 3);");
    expectNumber(1, "obj1.getSlot1()");
    expectNumber(11, "obj2.getSlot1()");
    expectNumber(111, "obj3.getSlot1()");
    expectNumber(2, "obj2.getSlot2()");
    expectNumber(22, "obj3.getSlot2()");
    expectNumber(3, "obj3.getSlot3()");
    expectNumber(1, "obj1.m()");
    expectNumber(12, "obj2.m()");
    expectNumber(113, "obj3.m()");
  }

  public void testStaticInitializer() throws Exception {
    loadClass("package2.TestStaticInitializer");
    complete();
    initClass("package2.TestStaticInitializer");
    expectString("s1", "package2.TestStaticInitializer.s1");
    expectString("s2/s1", "package2.TestStaticInitializer.s2");
    expectString("s3/s2/s1", "package2.TestStaticInitializer.s3");
    expectNumber(10, "package2.TestStaticInitializer.fv");
    // must not access private fields
    expectString("undefined", "typeof package2.TestStaticInitializer.f");
  }

  public void testStaticAccess() throws Exception {
    loadClass("package2.TestStaticAccess");
    complete();
    initClass("package2.TestStaticAccess");
    expectGetAndGetQualified("s1", true, "s1");
    expectGetAndGetQualified("s2", false, "s2");

    eval("package2.TestStaticAccess.s1='s1_mod1';");
    expectGetAndGetQualified("s1", true, "s1_mod1");
    eval("package2.TestStaticAccess.set_s1('s1_mod2');");
    expectGetAndGetQualified("s1", true, "s1_mod2");
    eval("package2.TestStaticAccess.set_s1_qualified('s1_mod3');");
    expectGetAndGetQualified("s1", true, "s1_mod3");
    eval("package2.TestStaticAccess.set_s1_fully_qualified('s1_mod4');");
    expectGetAndGetQualified("s1", true, "s1_mod4");

    eval("package2.TestStaticAccess.set_s2('s2_mod2');");
    expectGetAndGetQualified("s2", false, "s2_mod2");
    eval("package2.TestStaticAccess.set_s2_qualified('s2_mod3');");
    expectGetAndGetQualified("s2", false, "s2_mod3");
    eval("package2.TestStaticAccess.set_s2_fully_qualified('s2_mod4');");
    expectGetAndGetQualified("s2", false, "s2_mod4");

    // TODO: test that static members are *not* inherited.
  }

  public void testMemberNames() throws Exception {
    loadClass("package1.TestMemberNames");
    complete();
    eval("obj1 = new package1.TestMemberNames()");
    expectString("foo", "obj1.foo()");
    expectString("foo-static", "package1.TestMemberNames.foo()");
    expectString("bar-foo", "obj1.bar()");
    expectString("bar-foo-static", "package1.TestMemberNames.bar()");
  }

  private void expectGetAndGetQualified(String memberName, boolean publicMember, String expected) throws Exception {
    if (publicMember) {
      expectString(expected, "package2.TestStaticAccess."+memberName+"");
    } else {
      expectString("undefined", "typeof(package2.TestStaticAccess."+memberName+")");
    }
    expectString(expected, "package2.TestStaticAccess.get_"+memberName+"()");
    expectString(expected, "package2.TestStaticAccess.get_"+memberName+"_qualified()");
    expectString(expected, "package2.TestStaticAccess.get_"+memberName+"_fully_qualified()");
  }

  public void testInternal() throws Exception {
    loadClass("package2.TestInternal");
    complete();
    initClass("package2.TestInternal");
    expectString("internal", "new package2.TestInternal().returnInternal()");
  }

  public void testInitializeBeforeStaticMethod() throws Exception {
    loadClass("package2.TestStaticInitializer");
    complete();
    expectNumber(1, "package2.TestStaticInitializer.return1()");
    expectNumber(2, "package2.TestStaticInitializer.return2()");
  }

  public void testLocalVariables() throws Exception {
    loadClass("package1.TestLocalVariables");
    complete();
    eval("obj = new package1.TestLocalVariables();");
    expectNumber(200, "obj.m(10)");
    expectNumber(134, "obj.m2(10)");
  }

  public void testStatements() throws Exception {
    loadClass("package2.TestStatements");
    complete();
    eval("obj = new package2.TestStatements;");
    expectNumber(200, "obj.testIf(true, 200, 300)");
    expectNumber(300, "obj.testIf(false, 200, 300)");
    expectNumber(200, "obj.testIfThenElse(true, 200, 300)");
    expectNumber(300, "obj.testIfThenElse(false, 200, 300)");
    expectNumber(15, "obj.testWhile(5)");
    expectNumber(10, "obj.testFor(5)");
    expectNumber(15, "obj.testDoWhile(5)");
    expectString("x, y, z", "obj.testForIn({ y: 2, x :1, z: 3})");
    expectString("x, y, z", "obj.testForIn2({ y: 2, x :1, z: 3})");
    expectString("1, 2, 3", "obj.testForEach({ y: 2, x :1, z: 3})");
    expectString("1, 2, 3", "obj.testForEach2({ y: 2, x :1, z: 3})");
    expectString("1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3", "obj.testForEach3({ y: 2, x :1, z: 3})");
    expectNumber(11, "obj.testSwitch(1,1,11,2,22,33)");
    expectNumber(22, "obj.testSwitch(2,1,11,2,22,33)");
    expectNumber(33, "obj.testSwitch(3,1,11,2,22,33)");
    expectString("undefined", "typeof(obj.testReturnVoid())");
    expectNumber(42, "var o = { tobedeleted: 42 }; o.tobedeleted");
    expectNumber(42, "var o2 = { tobedeleted: 42 }; o2.tobedeleted");
    expectString("undefined", "obj.testDelete1(o); typeof(o.tobedeleted)");
    expectString("undefined", "obj.testDelete2(o2, 'tobedeleted'); typeof(o.tobedeleted)");
    expectString("is an Error: foo", "obj.testTryCatchFinally(new Error('foo'))");
    expectString("is not an Error: bar", "obj.testTryCatchFinally('bar')");
    expectBoolean(true, "obj.cleanedUp");
  }

  public void testExpressions() throws Exception {
    loadClass("package2.TestExpressions");
    complete();
    eval("obj = new package2.TestExpressions;");
    expectNumber(200, "obj.testCond(true, 200, 300)");
    expectNumber(300, "obj.testCond(false, 200, 300)");
    expectNumber(1, "obj.antitestRegexpLiterals()");
    expectString("foo", "obj.testCommaExprAsArrayIndex('foo')");
    expectNumber(24, "obj.testParenExpr(11)");
    expectNumber(7, "obj.testBinOpExpr(12)");
    expectNumber(130, "(obj.testFunExpr(13))(10)");
    expectNumber(-2, "obj.testPrefixOpExpr(14)");
    expectNumber(30, "obj.testPostfixOpExpr(15)");
    String dq = "'�\\\b\t\n\f\r\'/'\u00C6\u01Bfe\"'"; // "'�\\\b\t\n\f\r\'/'\xc6\u01Bfe\"'"
    String sq = "\"�\\\b\t\n\f\r\'/\"\u00C6\u01Bfe'\""; // '"�\\\b\t\n\f\r\'/"\xc6\u01Bfe\'"'
    expectString(dq, "obj.testStringLiteralsDQ()");
    expectString(sq, "obj.testStringLiteralsSQ()");
    expectString("ActionScript <span class='heavy'>3.0</span>", "obj.testStringLiterals3()");
    expectString("<item id=\"155\">banana</item>", "obj.testStringLiterals4()");
    expectString(dq, "obj.testCharLiterals()");
    expectString("2,7,2,2,2", "obj.testRegexpLiterals()");
    expectNumber(123+456, "obj.testObjectLiterals()");
    expectString("1,2,3,4,5,6,7,8,9,0", "obj.testArrayLiterals()");
  }

  public void testImport() throws Exception {
    loadClass("package2.TestImport");
    loadClass("package1.TestMethodCall");
    loadClass("package1.package11.TestSubPackage");
    loadClass("package2.TestStaticInitializer");
    complete();
    expectString("s2/s1"+"/"+(-19+42), "package2.TestImport.main()");
  }

  public void testInclude() throws Exception {
    loadClass("package2.TestInclude");
    complete();
    expectString("included!", "package2.TestInclude.testIncludeSameDir()");
    expectString("included!", "package2.TestInclude.testIncludeOtherDir()");
  }

  public void testSelfAwareness() throws Exception {
    runClass("package1.TestSelfAwareness");
  }

  public void testInitializers() throws Exception {
    doTestThreeSlots("package1.TestInitializers");
  }

  public void testInitializersWithDefaultConstructor() throws Exception {
    doTestThreeSlots("package1.TestInitializersWithDefaultConstructor");
  }

  public void testImplicitSuper() throws Exception {
    loadClass("package1.TestInitializers");
    doTestThreeSlots("package1.TestImplicitSuper");
  }

  public void testExplicitSuper() throws Exception {
    loadClass("package1.TestInitializers");
    doTestThreeSlots("package1.TestExplicitSuper");
  }

  private void doTestThreeSlots(String className) throws Exception {
    loadClass(className);
    complete();
    eval("obj = new "+className+"();");
    expectNumber(1, "obj.getSlot1()");
    expectNumber(2, "obj.getSlot2()");
    expectNumber(1, "obj.getSlot3().nolabel");
    expectNumber(2, "obj.getSlot3().alsonolabel");
    expectBoolean(true, "obj.getSlot3()===obj.getSlot3()");
    eval("obj2 = new "+className+"();");
    expectBoolean(false, "obj.getSlot3()===obj2.getSlot3()");
  }

  public void testUnqualifiedAccess() throws Exception {
    loadClass("package1.TestUnqualifiedAccess");
    complete();
    eval("obj = new package1.TestUnqualifiedAccess(\"a\")");
    eval("new package1.TestUnqualifiedAccess(\"c\")");
    expectString("a", "obj.getPrivateSlot()");
    eval("obj.setPrivateSlot(\"b\")");
    expectString("b", "obj.getPrivateSlot()");
    expectString("a", "obj.getProtectedSlot()");
    eval("obj.setProtectedSlot(\"b\")");
    expectString("b", "obj.getProtectedSlot()");
    expectString("a", "obj.getPublicSlot()");
    eval("obj.setPublicSlot(\"b\")");
    expectString("b", "obj.getPublicSlot()");

    expectBoolean(true, "obj.testConstructorAccess()");
    expectBoolean(true, "package1.TestUnqualifiedAccess.UNQUALIFIED_CLASS_EQUALS_QUALIFIED_CLASS");

    expectString("foo", "package1.TestUnqualifiedAccess.SET_BY_STATIC_INITIALIZER");

    expectBoolean(true, "obj.testLocalFunction()");

    expectString("foo", "obj.testForwardPrivateUnqualified('foo')");

    // TODO: test "unqualified access to super members"!
  }

  public void testTypeCast() throws Exception {
    loadClass("package2.TestInclude");
    loadClass("package1.TestTypeCast");
    complete();
    eval("obj = new package1.TestTypeCast()");
    expectBoolean(true, "package1.TestTypeCast.testAsCast(obj)===obj");
    expectBoolean(true, "package1.TestTypeCast.testFunctionCast(obj)===obj");
  }

  public void testNoSuper() throws Exception {
    loadClass("package1.TestNoSuper");
    try {
      complete();
    } catch (Exception e) {
      return;
    }
    fail("exception expected");
  }

  public void testYesSuper1() throws Exception {
    loadClass("package1.TestInheritanceSuperClass");
    loadClass("package1.TestInheritanceSubClass");
    loadClass("package1.TestInheritanceSubSubClass");
    complete();
  }

  public void testYesSuper2() throws Exception {
    loadClass("package1.TestInheritanceSubSubClass");
    loadClass("package1.TestInheritanceSubClass");
    loadClass("package1.TestInheritanceSuperClass");
    complete();
  }

  public void testParamInitializers() throws Exception {
    loadClass("package1.TestParamInitializers");
    complete();
    eval("obj = new package1.TestParamInitializers();");
    expectString("foo/bar", "obj.initParams1('foo','bar')");
    expectString("foo/1", "obj.initParams1('foo')");

    expectString("foo/bar/baz", "obj.initParams2('foo','bar','baz')");
    expectString("foo/bar/3", "obj.initParams2('foo','bar')");
    expectString("foo/foo/3", "obj.initParams2('foo')");
    expectString("bar/bar/3", "obj.initParams2()");

    expectString("foo/bar/3", "obj.initParams3('foo','bar','a',2,true)");
    expectString("foo/bar/0", "obj.initParams3('foo','bar')");
    expectString("foo/foo/0", "obj.initParams3('foo')");

    expectString("foo/bar/baz/3", "obj.initParams4('foo','bar','baz', 'a',2,true)");
    expectString("foo/bar/undefined/0", "obj.initParams4('foo','bar')");
    expectString("foo/foo/undefined/0", "obj.initParams4('foo')");
    expectString("undefined/foo/undefined/0", "obj.initParams4()");
  }

  public void testRestParams() throws Exception {
    loadClass("package1.TestRestParams");
    complete();
    eval("obj = new package1.TestRestParams();");
    expectNumber(3, "obj.anyParams(1,2,3);");
    expectNumber(3, "obj.anyParamsOptimized(1,2,3);");
    expectNumber(42, "obj.xAndAnyParams(40,2,3);");
  }

  public void testGetterSetter() throws Exception {
    loadClass("package1.TestGetterSetter");
    loadClass("package1.TestSetterOverride");
    complete();
    eval("obj = new package1.TestGetterSetter();");
    eval("obj.foo = '1234';");
    expectString("1234", "obj.foo");
    eval("obj.foo = 'foo';");
    expectString("NaN", "obj.foo");
    eval("sub = new package1.TestSetterOverride();");
    eval("sub.foo = '5678';");
    expectString("15678", "sub.foo");
  }

  public void testIs() throws Exception {
    loadClass("package1.TestIs");
    complete();
    expectBoolean(true, "package1.TestIs.testIs(new package1.TestIs(), package1.TestIs)");
    expectBoolean(false, "package1.TestIs.testIs(new package1.TestIs(), String)");
    expectBoolean(true, "package1.TestIs.testIs('foo', String)");
    expectBoolean(true, "package1.TestIs.testIs(new String('foo'), String)");
  }

  public void testInterface() throws Exception {
    loadClass("package1.TestInterface");
    loadClass("package1.TestImplements");
    loadClass("package1.TestInterface2");
    loadClass("package1.TestInheritImplements");
    complete();
    eval("obj = new package1.TestImplements();");
    expectNumber(5, "obj.implementMe('house')");
    expectBoolean(true, "joo.is(obj, package1.TestImplements)");
    expectBoolean(true, "joo.is(obj, package1.TestInterface)");
    expectBoolean(true, "joo.is(obj, Object)");
    expectBoolean(false, "joo.is(obj, Number)");
    eval("obj = new package1.TestInheritImplements();");
    expectBoolean(true, "joo.is(obj, package1.TestInheritImplements)");
    expectBoolean(true, "joo.is(obj, package1.TestInterface2)");
    expectBoolean(true, "joo.is(obj, package1.TestImplements)");
    expectBoolean(true, "joo.is(obj, package1.TestInterface)");
    expectBoolean(true, "joo.is(obj, Object)");
    expectBoolean(false, "joo.is(obj, Number)");

    // TODO: test inheritance between interfaces and implementing multiple interfaces at once.
  }

  public void testBind() throws Exception {
    loadClass("package1.TestBind");
    complete();
    eval("obj = new package1.TestBind('foo');");
    expectString("foo", "obj.getState()");
    expectString("foo", "obj.testInvokeLocalVar()");
    expectString("foo", "obj.testInvokeLocalVarUnqualified()");
    expectString("foo", "obj.testInvokeParameter()");
    expectString("foo", "obj.testInvokeParameterUnqualified()");
    expectString("foo", "obj.testInvokeParameterUnqualifiedPrivate()");
    expectString("foo", "obj.testLocalFunction()");
    expectString("foo", "obj.testLocalFunctionUnqualified()");
    expectString("bar", "obj.testNotBound.call({getState: function(){return 'bar';}})");
    expectBoolean(true, "package1.TestBind.testStaticNotBound().call('bar')=='bar'");
  }

  public void testMultiDeclarations() throws Exception {
    loadClass("package1.TestMultiDeclarations");
    complete();
    eval("obj = new package1.TestMultiDeclarations();");
    String expected = (String)eval("package1.TestMultiDeclarations.EXPECTED_RESULT");
    expectString(expected, "obj.testFields()");
    expectString(expected, "obj.testVariables()");
    expectString("[This is a test]", "obj.testForLoops(['This','is','a','test'])");
  }

  public void testJavaPackage() throws Exception {
    loadClass("net.jangaroo.test.JavaPackageTest");
    complete();
    eval("new net.jangaroo.test.JavaPackageTest();");
  }

  public void testNew() throws Exception {
    loadClass("package2.TestNew");
    complete();
    eval("obj = new package2.TestNew()");
    expectString("foo", "obj.foo()");
    expectString("foo", "obj.testNewCall()");
    expectString("foo", "obj.testNewCallNoThis()");
    expectString("foo", "package2.TestNew.testNewApplyExpr()");
    expectString("foo", "package2.TestNew.testNewExpr()");
    expectString("foo", "package2.TestNew.testNewStaticCall()");
    expectString("foo", "package2.TestNew.testNewFullyQualified()");
  }

  public static void main(String args[]) {
    junit.textui.TestRunner.run(JooTest.class);
  }

}