package daffodil.dsom

import daffodil.xml.XMLUtils
import daffodil.util._
import scala.xml._

import org.scalatest.junit.JUnit3Suite

import daffodil.schema.annotation.props.gen._
import daffodil.schema.annotation.props._
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue

class TestDsomCompiler extends JUnit3Suite {

  val xsd = XMLUtils.XSD_NAMESPACE
  val dfdl = XMLUtils.DFDL_NAMESPACE
  val xsi = XMLUtils.XSI_NAMESPACE
  val example = XMLUtils.EXAMPLE_NAMESPACE

  val dummyGroupRef = null // just because otherwise we have to construct too many things.

  def FindValue(collection: Map[String, String], key: String, value: String): Boolean = {
    val found: Boolean = Option(collection.find(x => x._1 == key && x._2 == value)) match {
      case Some(_) => true
      case None => false
    }
    found
  }

  // @Test
  def testHasProps() {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="list" type="tns:example1"/>
      <xs:complexType name="example1">
        <xs:sequence>
          <xs:element name="w" type="xs:int" dfdl:length="1" dfdl:lengthKind="explicit"/>
        </xs:sequence>
      </xs:complexType>)
    val compiler = Compiler()
    val (sset, _) = compiler.frontEnd(testSchema)
    val Seq(schema) = sset.schemas
    val Seq(schemaDoc) = schema.schemaDocuments
    val Seq(declf) = schemaDoc.globalElementDecls
    val decl = declf.forRoot()

    val df = schemaDoc.defaultFormat
    val tnr = df.textNumberRep
    assertEquals(TextNumberRep.Standard, tnr)
    val tnr2 = decl.textNumberRep
    assertEquals(TextNumberRep.Standard, tnr2)
    
  }

  // @Test
  def testSchemaValidationSubset() {
    val sch : Node = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="list">
        <xs:complexType>
          <xs:sequence maxOccurs="2">
            <!-- DFDL SUBSET DOESN'T ALLOW MULTIPLE RECURRING SEQUENCE OR CHOICE -->
            <xs:element name="w" type="xsd:int" dfdl:lengthKind="explicit" dfdl:length="{ 1 }"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>)

    val (sset, _) = Compiler().frontEnd(sch)
    assertTrue(sset.isError)
    val diagnostics = sset.getDiagnostics 
    val msgs = diagnostics.map{ _.getMessage }
    val msg = msgs.mkString("\n")
    val hasErrorText = msg.contains("maxOccurs");
    if (!hasErrorText) this.fail("Didn't get expected error. Got: " + msg)
  }

    // @Test
  def testTypeReferentialError() {
    val sch : Node = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="list" type="typeDoesNotExist"/>)
    val (sset, _) = Compiler().frontEnd(sch)
    assertTrue(sset.isError)
    val msg = sset.getDiagnostics.toString
    val hasErrorText = msg.contains("typeDoesNotExist");
    if (!hasErrorText) this.fail("Didn't get expected error. Got: " + msg)
  }
  
  // @Test
  def testSchemaValidationPropertyChecking() {
    val s : Node = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="list">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="w" type="xsd:int" dfdl:byteOrder="invalidValue"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>)
    val (sset, _) = Compiler().frontEnd(s)
    assertTrue(sset.isError)
    val msg = sset.getDiagnostics.toString
    val hasErrorText = msg.contains("invalidValue");
    if (!hasErrorText) this.fail("Didn't get expected error. Got: " + msg)
  }

  def test2() {
    val sc = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,

      <xs:element name="list" type="tns:example1">
        <xs:annotation>
          <xs:appinfo source={ dfdl }>
            <dfdl:element encoding="US-ASCII" alignmentUnits="bytes"/>
          </xs:appinfo>
        </xs:annotation>
      </xs:element>
      <xs:complexType name="example1">
        <xs:sequence dfdl:separator="">
          <xs:element name="w" type="xs:int" dfdl:length="1" dfdl:lengthKind="explicit"/>
        </xs:sequence>
      </xs:complexType>)

    val (sset, _) = Compiler().frontEnd(sc)

    val Seq(schema) = sset.schemas
    val Seq(schemaDoc) = schema.schemaDocuments
    val Seq(declFactory) = schemaDoc.globalElementDecls
    val decl = declFactory.forRoot()
    val Seq(ct) = schemaDoc.globalComplexTypeDefs
    assertEquals("example1", ct.name)

    val fa = decl.formatAnnotation.asInstanceOf[DFDLElement]
    assertEquals(AlignmentUnits.Bytes, fa.alignmentUnits)
    fa.alignmentUnits match {
      case AlignmentUnits.Bits => println("was bits")
      case AlignmentUnits.Bytes => println("was bytes")
    }
  }

  /* def testXsomMultifile(){
   
    val parser = new XSOMParser()
    val apf = new DomAnnotationParserFactory()
    parser.setAnnotationParser(apf)

    val inFile = new File(TestUtils.findFile("test/first.xsd"))

    parser.parse(inFile)

    val sset = parser.getResult()
    val sds = parser.getDocuments().toList
    assertTrue(sds.size() >= 2)
  
    sds.map{sd => println(sd.getSystemId)}
  }*/

  def testSequence1() {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,

      <xs:element name="list" type="tns:example1">
        <xs:annotation>
          <xs:appinfo source={ dfdl }>
            <dfdl:element encoding="US-ASCII" alignmentUnits="bytes"/>
          </xs:appinfo>
        </xs:annotation>
      </xs:element>
      <xs:complexType name="example1">
        <xs:sequence dfdl:separatorPolicy="required" dfdl:separator="">
          <xs:element name="w" type="xs:int" maxOccurs="1" dfdl:lengthKind="explicit" dfdl:length="1" dfdl:occursCountKind="fixed"/>
        </xs:sequence>
      </xs:complexType>)

    val w = Utility.trim(testSchema)

    val (sset, _) = Compiler().frontEnd(w)
    val Seq(schema) = sset.schemas
    val Seq(schemaDoc) = schema.schemaDocuments
    val Seq(decl) = schemaDoc.globalElementDecls
    val Seq(ct) = schemaDoc.globalComplexTypeDefs
    assertEquals("example1", ct.name)

    val mg = ct.forElement(null).modelGroup.asInstanceOf[Sequence]
    assertTrue(mg.isInstanceOf[Sequence])

    val Seq(elem) = mg.groupMembers
    assertTrue(elem.isInstanceOf[LocalElementDecl])

  }

  // @Test
  def testInputValueCalc1() {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="data" type="xs:string" dfdl:textNumberRep="standard" dfdl:representation="text" dfdl:terminator="" dfdl:emptyValueDelimiterPolicy="none" dfdl:inputValueCalc="{ 42 }" dfdl:initiator="" dfdl:lengthKind="explicit" dfdl:length="1"/>)
    val actual = Compiler.testString(testSchema, "")
    val actualString = actual.result.toString
    assertTrue(actualString.contains("<data"))
    assertTrue(actualString.contains(">42</data>"))
  }

  // @Test
  def testTerminator1() {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="data" type="xs:string" dfdl:terminator="!" dfdl:lengthKind="explicit" dfdl:length="{ 2 }"/>)
    val actual = Compiler.testString(testSchema, "37!")
    val actualString = actual.result.toString
    assertTrue(actualString.contains("<data"))
    assertTrue(actualString.contains(">37</data>"))
  }

  // @Test
  def testUnparse1() {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="data" type="xs:int" dfdl:occursStopValue="-1" dfdl:textNumberRep="standard" dfdl:terminator="" dfdl:emptyValueDelimiterPolicy="none" dfdl:initiator="" dfdl:lengthKind="explicit" dfdl:encoding="US-ASCII" dfdl:representation="text" dfdl:length="{ 2 }"/>)
    val compiler = Compiler()
    val pf = compiler.compile(testSchema)
    val unparser = pf.onPath("/")
    val outputStream = new java.io.ByteArrayOutputStream()
    val out = java.nio.channels.Channels.newChannel(outputStream)
    unparser.unparse(out, <data xmlns={ example }>37</data>)
    out.close()
    val actualString = outputStream.toString()
    assertEquals("37", actualString)
  }

  def test3 {
    val testSchema = XML.loadFile(TestUtils.findFile("test/example-of-most-dfdl-constructs.dfdl.xml"))
    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    // No annotations
    val Seq(ct) = sd.globalComplexTypeDefs

    // Explore global element decl
    val Seq(e1f, e2f, e3f, e4f, e5f) = sd.globalElementDecls // there are 3 factories
    val e1 = e1f.forRoot()
    val e2 = e2f.forRoot()
    val e3 = e3f.forRoot()
    assertEquals(
      ByteOrder.BigEndian.toString().toLowerCase(),
      e1.formatAnnotation.asInstanceOf[DFDLElement].getProperty("byteOrder").toLowerCase())
    val Seq(a1, a2) = e3.annotationObjs // third one has two annotations
    assertTrue(a2.isInstanceOf[DFDLNewVariableInstance]) // second annotation is newVariableInstance
    assertEquals(OccursCountKind.Implicit, a1.asInstanceOf[DFDLElement].occursCountKind)
    val e1ct = e1.immediateType.get.asInstanceOf[LocalComplexTypeDef] // first one has immediate complex type
    // Explore local complex type def
    val seq = e1ct.modelGroup.asInstanceOf[Sequence] //... which is a sequence
    val sfa = seq.formatAnnotation.asInstanceOf[DFDLSequence] //...annotated with...
    assertEquals(YesNo.No, sfa.initiatedContent) // initiatedContent="no"

    val Seq(e1a : DFDLElement) = e1.annotationObjs
    assertEquals("UTF-8", e1a.getProperty("encoding"))

    // Explore global simple type defs
    val Seq(st1, st2, st3, st4) = sd.globalSimpleTypeDefs // there are two.
    val Seq(b1, b2, b3, b4) = st1.forElement(e1).annotationObjs // first one has 4 annotations
    assertEquals(AlignmentUnits.Bytes, b1.asInstanceOf[DFDLSimpleType].alignmentUnits) // first has alignmentUnits
    assertEquals("tns:myVar1", b2.asInstanceOf[DFDLSetVariable].ref) // second is setVariable with a ref
    assertEquals("yadda yadda yadda", b4.asInstanceOf[DFDLAssert].message.get) // foruth is an assert with yadda message

    // Explore define formats
    val Seq(df1, df2) = sd.defineFormats // there are two
    val def1 = df1.asInstanceOf[DFDLDefineFormat]
    assertEquals("def1", def1.name) // first is named "def1"
    assertEquals(Representation.Text, def1.formatAnnotation.representation) // has representation="text"

    // Explore define variables
    val Seq(dv1, dv2) = sd.defineVariables // there are two
    //assertEquals("2003年08月27日", dv2.asInstanceOf[DFDLDefineVariable].defaultValue) // second has kanji chars in default value

    // Explore define escape schemes
    val Seq(desc1) = sd.defineEscapeSchemes // only one of these
    val es = desc1.escapeScheme.escapeCharacterRaw
    assertEquals("%%", es) // has escapeCharacter="%%" (note: string literals not digested yet, so %% is %%, not %.

    // Explore global group defs
    val Seq(gr1, gr2, gr3, gr4, gr5) = sd.globalGroupDefs // there are two
    val seq1 = gr1.forGroupRef(dummyGroupRef, 1).modelGroup.asInstanceOf[Sequence]

    //Explore LocalSimpleTypeDef
    val Seq(gr2c1, gr2c2, gr2c3) = gr2.forGroupRef(dummyGroupRef, 1).modelGroup.asInstanceOf[ModelGroup].groupMembers
    val ist = gr2c3.asInstanceOf[LocalElementDecl].immediateType.get.asInstanceOf[LocalSimpleTypeDef]
    assertEquals("tns:aType", ist.baseName)

    //Explore LocalElementDecl
    val led = gr2c1.asInstanceOf[LocalElementDecl]
    assertEquals(1, led.maxOccurs)
    val Seq(leda) = led.annotationObjs
    assertEquals("{ $myVar1 eq (+47 mod 4) }", leda.asInstanceOf[DFDLDiscriminator].testBody)

    // Explore sequence
    val Seq(seq1a : DFDLSequence) = seq1.annotationObjs // one format annotation with a property
    assertEquals(SeparatorPosition.Infix, seq1a.separatorPosition)
    val Seq(seq1e1, seq1s1) = seq1.groupMembers // has an element and a sub-sequence as its children.
    assertEquals(2, seq1e1.asInstanceOf[ElementRef].maxOccurs)
    assertEquals("ex:a", seq1e1.asInstanceOf[ElementRef].ref)
    assertEquals(0, seq1s1.asInstanceOf[Sequence].groupMembers.length)
  }

  def test4 {
    val testSchema = XML.loadFile(TestUtils.findFile("test/example-of-most-dfdl-constructs.dfdl.xml"))
    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(gd1, gd2, gd3, gd4, gd5) = sd.globalGroupDefs // Obtain Group nodes
    val ch1 = gd2.forGroupRef(dummyGroupRef, 1).modelGroup.asInstanceOf[Choice] // Downcast child-node of group to Choice
    val Seq(cd1, cd2, cd3) = ch1.groupMembers // Children nodes of Choice-node, there are 3

    val Seq(a1 : DFDLChoice) = gd2.forGroupRef(dummyGroupRef, 1).modelGroup.annotationObjs // Obtain the annotation object that is a child
    // of the group node.

    assertEquals(AlignmentType.Implicit, a1.alignment)
    assertEquals(ChoiceLengthKind.Implicit, a1.choiceLengthKind)

    val Seq(asrt1) = cd2.asInstanceOf[LocalElementDecl].annotationObjs // Obtain Annotation object that is child
    // of cd2.

    assertEquals("{ $myVar1 eq xs:int(xs:string(fn:round-half-to-even(8.5))) }", asrt1.asInstanceOf[DFDLAssert].test.get)

  }

  val testSchema =
    XML.loadFile(
      TestUtils.findFile(
        "test/example-of-named-format-chaining-and-element-simpleType-property-combining.dfdl.xml"))

  def test_named_format_chaining {
    val compiler = Compiler()
    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f, ge2f, ge3f, ge4f, ge5f, ge6f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()
    val Seq(a1 : DFDLElement) = ge1.annotationObjs

    val props : Map[String, String] = a1.getFormatProperties()

    def foundValues(collection : Map[String, String], key : String, value : String) : Boolean = {
      val found : Boolean = Option(collection.find(x => x._1 == key && x._2 == value)) match {
        case Some(_) => true
        case None => false
      }
      found
    }

    assertEquals(true, foundValues(props, "occursCountKind", "parsed"))
    assertEquals(true, foundValues(props, "lengthKind", "pattern"))
    assertEquals(true, foundValues(props, "representation", "text"))
    assertEquals(true, foundValues(props, "binaryNumberRep", "packed"))

  }

  def test_simple_types_access_works {
    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1, ge2, ge3, ge4, ge5, ge6) = sd.globalElementDecls // Obtain global element nodes

    val x = ge2.forRoot().typeDef.asInstanceOf[LocalSimpleTypeDef]

    assertEquals(AlignmentUnits.Bytes, x.alignmentUnits)
  }

  def test_simple_types_property_combining {

    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f, ge2f, ge3f, ge4f, ge5f, ge6f) = sd.globalElementDecls // Obtain global element nodes

    val ge2 = ge2f.forRoot()
    val ge3 = ge3f.forRoot()
    val ge4 = ge4f.forRoot()
    val ge5 = ge5f.forRoot()
    val ge6 = ge6f.forRoot()

    assertEquals(AlignmentUnits.Bytes, ge2.alignmentUnits)

    assertEquals(AlignmentUnits.Bytes, ge3.alignmentUnits)
    assertEquals(NilKind.LiteralValue, ge3.nilKind)

    // Tests overlapping properties
    intercept[daffodil.exceptions.SDE] { ge4.lengthKind }

    assertEquals(AlignmentUnits.Bytes, ge5.alignmentUnits) // local
    assertEquals(OccursCountKind.Parsed, ge5.occursCountKind) // def1
    assertEquals(BinaryNumberRep.Bcd, ge5.binaryNumberRep) // def3
    assertEquals(NilKind.LiteralValue, ge5.nilKind) // local
    assertEquals(Representation.Text, ge5.representation) // def3
    assertEquals(LengthKind.Pattern, ge5.lengthKind) // superseded by local

    // Test Defaulting
    assertEquals(BinaryNumberRep.Packed, ge6.binaryNumberRep)
  }

  def testTerminatingMarkup {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="e1" dfdl:lengthKind="implicit">
        <xs:complexType>
          <xs:sequence dfdl:separator=",">
            <xs:element name="s1" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="{ 1 }" minOccurs="0" dfdl:occursCountKind="parsed" dfdl:terminator=";"/>
            <xs:element name="s2" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="{ 1 }"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>)
    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments
    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()
    val ct = ge1.immediateType.get.asInstanceOf[LocalComplexTypeDef]
    val sq = ct.modelGroup.group.asInstanceOf[Sequence]
    val Seq(s1, s2) = sq.groupMembers.asInstanceOf[List[LocalElementDecl]]
    val s1tm = s1.terminatingMarkup
    val Seq(ce) = s1tm
    assertTrue(ce.isConstant)
    assertEquals(";", ce.constant)
  }

  def testTerminatingMarkup2 {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1"/>,
      <xs:element name="e1" dfdl:lengthKind="implicit">
        <xs:complexType>
          <xs:sequence dfdl:separator="," dfdl:separatorPosition="infix" dfdl:separatorPolicy="required" dfdl:terminator=";">
            <xs:element name="s1" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="{ 1 }" minOccurs="0" dfdl:occursCountKind="parsed"/>
            <xs:element name="s2" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="{ 1 }"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>)
    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()
    val ct = ge1.immediateType.get.asInstanceOf[LocalComplexTypeDef]
    val sq = ct.modelGroup.group.asInstanceOf[Sequence]
    val Seq(s1, s2) = sq.groupMembers.asInstanceOf[List[LocalElementDecl]]
    val s1tm = s1.terminatingMarkup
    val Seq(ce) = s1tm
    assertTrue(ce.isConstant)
    assertEquals(",", ce.constant)
    val s2tm = s2.terminatingMarkup
    val Seq(ce1, ce2) = s2tm
    assertTrue(ce1.isConstant)
    assertEquals(",", ce1.constant)
    assertTrue(ce2.isConstant)
    assertEquals(";", ce2.constant)
  }

  def test_simpleType_base_combining {
    // TO-DO: Add foundValues to a utils section or declare it at top of file?
    //
    def foundValues(collection : Map[String, String], key : String, value : String) : Boolean = {
      val found : Boolean = Option(collection.find(x => x._1 == key && x._2 == value)) match {
        case Some(_) => true
        case None => false
      }
      found
    }

    val testSchema = XML.loadFile(TestUtils.findFile("test/example-of-most-dfdl-constructs.dfdl.xml"))
    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    // No annotations
    val Seq(ct) = sd.globalComplexTypeDefs

    // Explore global element decl
    val Seq(e1f, e2f, e3f, e4f, e5f) = sd.globalElementDecls // there are 3 factories
    val e1 = e1f.forRoot()
    val e2 = e2f.forRoot()
    val e3 = e3f.forRoot()

    val Seq(gs1f, gs2f, gs3f, gs4f) = sd.globalSimpleTypeDefs

    val gs1 = gs1f.forRoot() // Global Simple Type - aType

    assertEquals("ex:aaType", gs1.restrictionBase)
    assertTrue(foundValues(gs1.allNonDefaultProperties, "alignmentUnits", "bytes")) // SimpleType - Local
    assertTrue(foundValues(gs1.allNonDefaultProperties, "byteOrder", "bigEndian")) // SimpleType - Base
    assertTrue(foundValues(gs1.allNonDefaultProperties, "occursCountKind", "implicit")) // Default Format
    assertTrue(foundValues(gs1.allNonDefaultProperties, "representation", "text")) // Define Format - def1
    assertTrue(foundValues(gs1.allNonDefaultProperties, "encoding", "utf-8")) // Define Format - def1
    assertTrue(foundValues(gs1.allNonDefaultProperties, "textStandardBase", "10")) // Define Format - def2
    assertTrue(foundValues(gs1.allNonDefaultProperties, "escapeSchemeRef", "tns:quotingScheme")) // Define Format - def2

    val gs3 = gs3f.forRoot() // Global SimpleType - aTypeError - overlapping base props

    // Tests overlapping properties
    intercept[daffodil.exceptions.SDE] { gs3.allNonDefaultProperties }
  }

  def test_group_references {
    // TO-DO: Add foundValues to a utils section or declare it at top of file?
    //
    def foundValues(collection : Map[String, String], key : String, value : String) : Boolean = {
      val found : Boolean = Option(collection.find(x => x._1 == key && x._2 == value)) match {
        case Some(_) => true
        case None => false
      }
      found
    }

    val testSchema = XML.loadFile(TestUtils.findFile("test/example-of-most-dfdl-constructs.dfdl.xml"))
    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    // No annotations
    val Seq(ct) = sd.globalComplexTypeDefs

    // Explore global element decl
    val Seq(e1f, e2f, e3f, e4f, e5f) = sd.globalElementDecls // there are 3 factories

    // GroupRefTest
    val e4 = e4f.forRoot() // groupRefTest

    val e4ct = e4.immediateType.get.asInstanceOf[LocalComplexTypeDef]

    val e4ctgref = e4ct.modelGroup.asInstanceOf[GroupRef] // groupRefTests' local group decl

    val myGlobal1 = e4ctgref.groupDef

    val myGlobal1Seq = myGlobal1.modelGroup.asInstanceOf[Sequence]

    val myGlobal2Seq = myGlobal1Seq.immediateGroup.get.asInstanceOf[Sequence]

    // val myGlobal2Seq = myGlobal2.modelGroup.asInstanceOf[Sequence]

    // myGlobal1 Properties
    assertTrue(foundValues(myGlobal1Seq.allNonDefaultProperties, "separator", ","))

    // myGlobal2 Properties
    assertTrue(foundValues(myGlobal2Seq.allNonDefaultProperties, "separator", ";"))
    assertTrue(foundValues(myGlobal2Seq.allNonDefaultProperties, "separatorPosition", "infix"))

    // GroupRefTestOverlap
    val e5 = e5f.forRoot() // groupRefTestOverlap

    val e5ct = e5.immediateType.get.asInstanceOf[LocalComplexTypeDef]

    val e5ctgref = e5ct.modelGroup.asInstanceOf[GroupRef] // groupRefTestOverlap's local group decl

    val myGlobal3 = e5ctgref.groupDef
    val myGlobal3Seq = myGlobal3.modelGroup.asInstanceOf[Sequence]

    // Tests overlapping properties
    intercept[daffodil.exceptions.SDE] { myGlobal3Seq.allNonDefaultProperties }

  }

  val ibm7132Schema = XML.loadFile(TestUtils.findFile("test/TestRefChainingIBM7132.dfdl.xml"))

  def test_ibm_7132 {
    val compiler = Compiler()
    val sset = new SchemaSet(ibm7132Schema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()

    val f1 = ge1.formatAnnotation

    val props : Map[String, String] = f1.getFormatProperties()

    def foundValues(collection : Map[String, String], key : String, value : String) : Boolean = {
      val found : Boolean = Option(collection.find(x => x._1 == key && x._2 == value)) match {
        case Some(_) => true
        case None => false
      }
      found
    }

    assertEquals(true, foundValues(props, "separatorPosition", "infix"))
    assertEquals(true, foundValues(props, "lengthKind", "implicit"))
    assertEquals(true, foundValues(props, "representation", "text"))
    assertEquals(true, foundValues(props, "textNumberRep", "standard"))

    val ct = ge1.typeDef.asInstanceOf[ComplexTypeBase]
    val seq = ct.modelGroup.asInstanceOf[Sequence]

    val Seq(e1 : ElementBase, e2 : ElementBase) = seq.groupMembers

    val e1f = e1.formatAnnotation.asInstanceOf[DFDLElement]
    val e1fProps : Map[String, String] = e1f.getFormatProperties()

    //    println(e1fProps)
    //
    def e1fValues(collection : Map[String, String], key : String, value : String) : Boolean = {
      val found : Boolean = Option(collection.find(x => x._1 == key && x._2 == value)) match {
        case Some(_) => true
        case None => false
      }
      found
    }
    assertEquals(true, e1fValues(e1fProps, "initiator", ""))
    //println(e1f.initiatorRaw)

    //e1f.initiatorRaw
    //e1f.byteOrderRaw
    e1f.lengthKind

  }

  def testDfdlRef = {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:defineFormat name="ref1"> <dfdl:format initiator=":"/> </dfdl:defineFormat>,
      <xs:element name="e1" dfdl:lengthKind="implicit" dfdl:ref="tns:ref1" type="xs:string">
      </xs:element>)
    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()
    val props = ge1.formatAnnotation.getFormatProperties()

    println(props)
    //assertEquals(":", ge1.initiatorRaw)
  }

  def testGetQName = {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:defineFormat name="ref1">
        <dfdl:format initiator=":"/>
      </dfdl:defineFormat>,
      <xs:element name="e1" dfdl:lengthKind="implicit" dfdl:ref="tns:ref1" type="xs:string">
      </xs:element>)
    println(testSchema)
    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()
    //val props = ge1.formatAnnotation.getFormatProperties()

    val (nsURI, localName) = ge1.formatAnnotation.getQName("ref1")

    println(nsURI + ", " + localName)
  }

  def testGetAllNamespaces() {
    val xml = <bar xmlns:foo="fooNS" xmlns:bar="barNS">
                <quux xmlns:baz="bazNS" attr1="x"/>
              </bar>

    val scope = (xml \ "quux")(0).scope
    println(scope)
    val newElem = scala.xml.Elem("dfdl", "element", null, scope)
    println(newElem)
  }

  val delimiterInheritance = XML.loadFile(TestUtils.findFile("test/TestDelimiterInheritance.dfdl.xml"))

  def test_delim_inheritance {
    val compiler = Compiler()
    val sset = new SchemaSet(delimiterInheritance)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()

    val ct = ge1.typeDef.asInstanceOf[ComplexTypeBase]
    val seq = ct.modelGroup.asInstanceOf[Sequence]

    val Seq(e1 : ElementBase, e2 : ElementBase, e3 : ElementBase) = seq.groupMembers

    //assertEquals(e1.terminatingMarkup, List("a", "b")) // 1 Level
    println(e1.terminatingMarkup)

    val ct2 = e3.asInstanceOf[ElementBase].typeDef.asInstanceOf[ComplexTypeBase]
    val seq2 = ct2.modelGroup.asInstanceOf[Sequence]
    val Seq(e3_1 : ElementBase, e3_2 : ElementBase) = seq2.groupMembers

    println(e3_1.terminatingMarkup)
    println(e3_2.terminatingMarkup)
    // assertEquals(e3_1.terminatingMarkup, List("e", "c", "d", "a", "b")) // 2 Level
    // assertEquals(e3_2.terminatingMarkup, List("f", "c", "d", "a", "b")) // 2 Level + ref
  }

  def test_escapeSchemeOverride = {
    val testSchema = TestUtils.dfdlTestSchema(
      <dfdl:format separator="" initiator="" terminator="" emptyValueDelimiterPolicy="none" textNumberRep="standard" representation="text" occursStopValue="-1" occursCountKind="expression" escapeSchemeRef="pound"/>
      <dfdl:defineEscapeScheme name="pound">
        <dfdl:escapeScheme escapeCharacter='#' escapeKind="escapeCharacter"/>
      </dfdl:defineEscapeScheme>
      <dfdl:defineEscapeScheme name='cStyleComment'>
        <dfdl:escapeScheme escapeBlockStart='/*' escapeBlockEnd='*/' escapeKind="escapeBlock"/>
      </dfdl:defineEscapeScheme>,
      <xs:element name="list">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="character" type="xsd:string" maxOccurs="unbounded" dfdl:representation="text" dfdl:separator="," dfdl:terminator="%NL;" />
            <xs:element name="block" type="xsd:string" maxOccurs="unbounded" dfdl:representation="text" dfdl:separator="," dfdl:terminator="%NL;" dfdl:escapeSchemeRef="cStyleComment"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>)
    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    val Seq(ge1f) = sd.globalElementDecls // Obtain global element nodes
    val ge1 = ge1f.forRoot()

    val ct = ge1.typeDef.asInstanceOf[ComplexTypeBase]
    val seq = ct.modelGroup.asInstanceOf[Sequence]

    val Seq(e1: ElementBase, e2: ElementBase) = seq.groupMembers
    val e1f = e1.formatAnnotation.asInstanceOf[DFDLElement]
    val props = e1.localAndFormatRefProperties ++ e1.defaultProperties
    
    val e1f_esref = e1.getProperty("escapeSchemeRef")
    println(e1f_esref)
    
    assertEquals("pound", e1f_esref)
    
    // Should have escapeCharacter and escapeKind
    
    val e2f = e2.formatAnnotation.asInstanceOf[DFDLElement]
    val e2f_esref = e2.getProperty("escapeSchemeRef")
    // escapeBlockStart/End escapeBlockKind (NOTHING ELSE)
    assertEquals("cStyleComment", e2f_esref)
  }
  
  def test_element_references {
    val testSchema = XML.loadFile(TestUtils.findFile("test/example-of-most-dfdl-constructs.dfdl.xml"))
    val compiler = Compiler()

    val sset = new SchemaSet(testSchema)
    val Seq(sch) = sset.schemas
    val Seq(sd) = sch.schemaDocuments

    // No annotations
    val Seq(ct) = sd.globalComplexTypeDefs

    // g1.name == "gr"
    val Seq(g1: GlobalGroupDefFactory, g2, g3, g4, g5) = sd.globalGroupDefs
  
    val seq1 = g1.forGroupRef(dummyGroupRef, 1).modelGroup.asInstanceOf[Sequence]
    
    // e1.ref == "ex:a"
    val Seq(e1: ElementRef, s1: Sequence) = seq1.groupMembers
    
    assertEquals(2, e1.maxOccurs)
    assertEquals(1, e1.minOccurs)
    assertEquals(AlignmentUnits.Bytes, e1.alignmentUnits)
    //assertEquals(true, e1.nillable) // TODO: e1.nillable doesn't exist?
    //assertEquals("%ES; %% %#0; %NUL;%ACK; foo%#rF2;%#rF7;bar %WSP*; %#2024;%#xAABB; &amp;&#2023;&#xCCDD; -1", e1.nilValue) // TODO: Do not equal each other!
    assertEquals(NilKind.LiteralValue, e1.nilKind)
  }

}

