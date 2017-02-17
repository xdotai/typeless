/*
 * Copyright (c) 2015-6 x.ai inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.x
package typeless
package test

import org.scalatest._
import shapeless._
import typeless.hlist._

class CrunchTests extends FunSuite with Matchers {
  val ls =
    { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      { (i: Int, s: String) => s"$i + $s" } ::
      HNil
  val all =
    ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      ls ::
      HNil

  import ApplyEachSeq.Ops

  test("apply all") {
    val res: Seq[String] = all.runAllSeq(1, "a")
    assert(res.distinct === Seq("1 + a"))
  }

}

