/* Copyright (c) 2012-2015 Tresys Technology, LLC. All rights reserved.
 *
 * Developed by: Tresys Technology, LLC
 *               http://www.tresys.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal with
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimers.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimers in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. Neither the names of Tresys Technology, nor the names of its contributors
 *     may be used to endorse or promote products derived from this Software
 *     without specific prior written permission.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
 * SOFTWARE.
 */

package edu.illinois.ncsa.daffodil.processors

import java.nio.ByteBuffer
import edu.illinois.ncsa.daffodil.dsom._
import edu.illinois.ncsa.daffodil.grammar.Terminal
import edu.illinois.ncsa.daffodil.util.LogLevel
import edu.illinois.ncsa.daffodil.exceptions.Assert
import edu.illinois.ncsa.daffodil.exceptions.UnsuppressableException
import edu.illinois.ncsa.daffodil.util.Maybe
import edu.illinois.ncsa.daffodil.util.Maybe._
import edu.illinois.ncsa.daffodil.processors.parsers.LiteralNilExplicitLengthInBytesParser
import edu.illinois.ncsa.daffodil.processors.parsers.LiteralNilKnownLengthInBytesParser
import edu.illinois.ncsa.daffodil.processors.parsers.LiteralNilExplicitLengthInCharsParser
import edu.illinois.ncsa.daffodil.processors.parsers.LiteralNilPatternParser
import edu.illinois.ncsa.daffodil.processors.unparsers.LiteralNilPatternUnparser

case class LiteralNilExplicitLengthInBytes(e: ElementBase)
  extends LiteralNilInBytesBase(e, "LiteralNilExplicit") {

  val expr = e.length
  val exprText = expr.prettyExpr

  override lazy val parser: PrimParser = new LiteralNilExplicitLengthInBytesParser(
    parsingPadChar,
    justificationTrim,
    e.elementRuntimeData,
    e.name,
    e.length,
    new ListOfStringValueAsLiteral(e.nilValue, e).cooked)

}

case class LiteralNilKnownLengthInBytes(e: ElementBase, lengthInBytes: Long)
  extends LiteralNilInBytesBase(e, "LiteralNilKnown") {

  final def computeLength(start: PState) = {
    (lengthInBytes, start.variableMap)
  }

  override lazy val parser: PrimParser = new LiteralNilKnownLengthInBytesParser(
    parsingPadChar,
    justificationTrim: TextJustificationType.Type,
    lengthInBytes: Long,
    e.elementRuntimeData,
    e.name,
    new ListOfStringValueAsLiteral(e.nilValue, e).cooked)

}

abstract class LiteralNilInBytesBase(e: ElementBase, label: String)
  extends StaticText(e.nilValue, e, e, label, e.isNillable)
  with Padded {

  val charset = e.knownEncodingCharset

  // We are to assume that we can always read nBytes
  // a failure to read nBytes is a failure period.

  lazy val unparserDelim = Assert.notYetImplemented()

}

case class LiteralNilExplicitLengthInChars(e: ElementBase)
  extends StaticText(e.nilValue, e, e, "LiteralNilExplicit", e.isNillable)
  with Padded {

  val charset = e.knownEncodingCharset
  // We are to assume that we can always read nChars
  // a failure to read nChars is a failure period.

  // TODO: LiteralNilExplicitLengthInChars really is a variation of LiteralNilPattern
  lazy val unparserDelim = Assert.notYetImplemented()

  override lazy val parser = new LiteralNilExplicitLengthInCharsParser(
    parsingPadChar,
    justificationTrim: TextJustificationType.Type,
    e.elementRuntimeData,
    e.name,
    e.length,
    new ListOfStringValueAsLiteral(e.nilValue, e).cooked)

}

//case class LiteralNilExplicit(e: ElementBase, nUnits: Long)
//  extends StaticText(e.nilValue, e, e, "LiteralNilExplicit", e.isNillable)
//  with Padded {
//  val charset = e.knownEncodingCharset
//
//  lazy val unparserDelim = Assert.notYetImplemented()
//  //val stParser = super.parser
//
//  override lazy val parser = new LiteralNilExplicitParser(
//    parsingPadChar,
//    justificationTrim: TextJustificationType.Type,
//    nUnits: Long,
//    e.elementRuntimeData,
//    e.name,
//    e.lengthPattern,
//    new ListOfStringValueAsLiteral(e.nilValue, e).cooked)
//
//}

case class LiteralNilPattern(e: ElementBase)
  extends StaticText(e.nilValue, e, e, "LiteralNilPattern", e.isNillable)
  with Padded {
  val charset = e.knownEncodingCharset
  lazy val unparserDelim = Assert.notYetImplemented()
  //val stParser = super.parser

  override lazy val parser = new LiteralNilPatternParser(
    parsingPadChar,
    justificationTrim: TextJustificationType.Type,
    e.elementRuntimeData,
    e.lengthPattern,
    e.name,
    e.nilValues)

  override lazy val unparser = new LiteralNilPatternUnparser(e.elementRuntimeData, e.nilValues.head)
}

case class LogicalNilValue(e: ElementBase) extends Primitive(e, e.isNillable)
