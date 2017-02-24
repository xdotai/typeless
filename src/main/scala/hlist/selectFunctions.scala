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
package hlist

import shapeless._
import ops.hlist.SelectAll
import ops.adjoin.Adjoin
import syntax.std.function._
import ops.function._
import shapeless.ops.coproduct.Inject

/* takes an HList of functions and an HList of potential arguments
   * it applies the arguments to the functions for which all the arguments are present
   * it return an HList with the results
   */
trait SelectFunctions[FF <: HList, Context <: HList] {
  type Out <: HList
  def apply(fs: FF, context: Context)(implicit distinct: IsDistinctConstraint[Context]): Out
}

trait SelectFunctions0 {
  implicit def hconsNotFound[F, FF <: HList, Context <: HList, Args <: HList, RT <: HList](
    implicit
    applyContext: SelectFunctions.Aux[FF, Context, RT]
  ): SelectFunctions.Aux[F :: FF, Context, RT] = new SelectFunctions[F :: FF, Context] {
    type Out = RT
    def apply(fs: F :: FF, context: Context)(implicit distinct: IsDistinctConstraint[Context]) =
      applyContext(fs.tail, context)
  }

}
object SelectFunctions extends SelectFunctions0 {
  type Aux[FF <: HList, Context <: HList, R <: HList] = SelectFunctions[FF, Context] { type Out = R }
  implicit def hcons[F, FF <: HList, Context <: HList, Args <: HList, R, RT <: HList](
    implicit
    fp: FnToProduct.Aux[F, Args => R],
    subset: SelectAll[Context, Args],
    applyContext: SelectFunctions.Aux[FF, Context, RT]
  ): SelectFunctions.Aux[F :: FF, Context, R :: RT] = new SelectFunctions[F :: FF, Context] {
    type Out = R :: RT
    def apply(fs: F :: FF, context: Context)(implicit distinct: IsDistinctConstraint[Context]) =
      fs.head.toProduct(subset(context)) :: applyContext(fs.tail, context)
  }

  implicit def hnil[Context <: HList]: SelectFunctions.Aux[HNil, Context, HNil] = new SelectFunctions[HNil, Context] {
    type Out = HNil
    def apply(fs: HNil, context: Context)(implicit distinct: IsDistinctConstraint[Context]) = HNil
  }

  def applyAll[Context <: Product, HContext <: HList, FF <: HList, R](context: Context)(fs: FF)(
    implicit
    gen: Generic.Aux[Context, HContext],
    selectFunctions: SelectFunctions[FF, HContext],
    distinct: IsDistinctConstraint[HContext]
  ) = selectFunctions(fs, gen.to(context))

  def applyAll[X, FF <: HList, R](x: X)(fs: FF)(
    implicit
    selectFunctions: SelectFunctions[FF, X :: HNil]
  ) = selectFunctions(fs, x :: HNil)
}

/* takes an HList of HLists of functions and an HList of potential arguments, 
 * and uses SelectFunctions[Context, FF] to calculate the resulting HList
 */
trait FlattenFunctions[Context <: HList, FFF <: HList] {
  type Out <: HList
  def apply(fs: FFF, args: Context)(implicit distinct: IsDistinctConstraint[Context]): Out
}

trait FlattenFunctions0 {
  implicit def hcons[Context <: HList, FF <: HList, FFF <: HList, SR <: HList](
    implicit
    flattenFunctions: FlattenFunctions.Aux[Context, FFF, SR]
  ): FlattenFunctions.Aux[Context, FF :: FFF, SR] = new FlattenFunctions[Context, FF :: FFF] {
    type Out = SR
    def apply(fs: FF :: FFF, args: Context)(implicit distinct: IsDistinctConstraint[Context]) =
      flattenFunctions(fs.tail, args)
  }
}

object FlattenFunctions extends FlattenFunctions0 {
  type Aux[Context <: HList, FFF <: HList, R <: HList] = FlattenFunctions[Context, FFF] { type Out = R }
  implicit def hcons[Context <: HList, FF <: HList, FFF <: HList, R <: HList, SR <: HList](
    implicit
    selectFunctions: SelectFunctions.Aux[FF, Context, R],
    flattenFunctions: FlattenFunctions.Aux[Context, FFF, SR]
  ): FlattenFunctions.Aux[Context, FF :: FFF, R :: SR] = new FlattenFunctions[Context, FF :: FFF] {
    type Out = R :: SR
    def apply(fs: FF :: FFF, args: Context)(implicit distinct: IsDistinctConstraint[Context]) =
      selectFunctions(fs.head, args) :: flattenFunctions(fs.tail, args)
  }
  implicit def hnil[Context <: HList]: FlattenFunctions.Aux[Context, HNil, HNil] =
    new FlattenFunctions[Context, HNil] {
      type Out = HNil
      def apply(fs: HNil, args: Context)(implicit distinct: IsDistinctConstraint[Context]) = HNil
    }

  def applyAll[Context <: Product, HContext <: HList, FFF <: HList, RR <: HList](args: Context)(fs: FFF)(
    implicit
    gen: Generic.Aux[Context, HContext],
    flattenFunctions: FlattenFunctions.Aux[HContext, FFF, RR],
    adj: Adjoin[RR],
    distinct: IsDistinctConstraint[HContext]
  ) = flattenFunctions(fs, gen.to(args)).adjoined
}

/* takes an HList of functions, which all return the same type, and an HList of potential arguments
   * it applies the arguments to the functions for which all the arguments are present
   * it return an Seq with the results
   */
trait SelectFunctionsSeq[FF <: HList, Context <: HList] {
  type Out
  def apply(fs: FF, context: Context)(implicit distinct: IsDistinctConstraint[Context]): Seq[Out]
}

object SelectFunctionsSeq {
  type Aux[FF <: HList, Context <: HList, R] = SelectFunctionsSeq[FF, Context] { type Out = R }
  implicit def hcons[Context <: HList, FF <: HList, F, Args <: HList, R](
    implicit
    fp: FnToProduct.Aux[F, Args => R],
    subset: Subset[Context, Args],
    selectFunctionsTail: SelectFunctionsSeq.Aux[FF, Context, R]
  ): SelectFunctionsSeq.Aux[F :: FF, Context, R] = new SelectFunctionsSeq[F :: FF, Context] {
    type Out = R
    def apply(fs: F :: FF, context: Context)(implicit distinct: IsDistinctConstraint[Context]): Seq[Out] = {
      subset(context).map(args => fs.head.toProduct(args)).toSeq ++
        selectFunctionsTail(fs.tail, context)
    }
  }
  implicit def hnil[Context <: HList, R]: SelectFunctionsSeq.Aux[HNil, Context, R] = new SelectFunctionsSeq[HNil, Context] {
    type Out = R
    def apply(fs: HNil, context: Context)(implicit distinct: IsDistinctConstraint[Context]): Seq[Out] = Seq.empty
  }

  def applyAll[Context <: Product, HContext <: HList, FF <: HList, R](context: Context)(fs: FF)(
    implicit
    gen: Generic.Aux[Context, HContext],
    selectFunctions: SelectFunctionsSeq.Aux[FF, HContext, R],
    distinct: IsDistinctConstraint[HContext]
  ): Seq[R] = selectFunctions(fs, gen.to(context))

  def applyAll[X, FF <: HList, R](x: X)(fs: FF)(
    implicit
    selectFunctions: SelectFunctionsSeq.Aux[FF, X :: HNil, R]
  ): Seq[R] = selectFunctions(fs, x :: HNil)

}

/* takes an HList of HLists FFF of functions and an HList of potential arguments Context, 
   * it uses SelectFunctionsSeq[Context, FF] (FF is an HList of functions) to calculate Seq[R]. 
   * Meaning all functions most return the same type R. 
   */
trait FlattenFunctionsSeq[Context <: HList, FFF <: HList] {
  type Out
  def apply(fs: FFF, args: Context)(implicit distinct: IsDistinctConstraint[Context]): Seq[Out]
}

object FlattenFunctionsSeq {
  type Aux[Context <: HList, FFF <: HList, R] = FlattenFunctionsSeq[Context, FFF] { type Out = R }
  implicit def hcons[Context <: HList, FF <: HList, FFF <: HList, R](
    implicit
    selectFunctions: SelectFunctionsSeq.Aux[FF, Context, R],
    flattenFunctions: FlattenFunctionsSeq.Aux[Context, FFF, R]
  ) = new FlattenFunctionsSeq[Context, FF :: FFF] {
    type Out = R
    def apply(fs: FF :: FFF, args: Context)(implicit distinct: IsDistinctConstraint[Context]) =
      selectFunctions(fs.head, args) ++ flattenFunctions(fs.tail, args)
  }
  implicit def hnil[Context <: HList, R] =
    new FlattenFunctionsSeq[Context, HNil] {
      type Out = R
      def apply(fs: HNil, args: Context)(implicit distinct: IsDistinctConstraint[Context]) = Seq.empty
    }

  def applyAll[Context <: Product, HContext <: HList, FFF <: HList, R](args: Context)(fs: FFF)(
    implicit
    gen: Generic.Aux[Context, HContext],
    flattenFunctions: FlattenFunctionsSeq.Aux[HContext, FFF, R],
    distinct: IsDistinctConstraint[HContext]
  ): Seq[R] = flattenFunctions(fs, gen.to(args))
}