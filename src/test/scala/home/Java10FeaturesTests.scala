package home

import java10.{DynamicProxyTests, Java10Features}
import org.scalatest.FunSuite

class Java10FeaturesTests extends FunSuite {
    test("...") {
        val java9Features = new Java10Features
        java9Features.feature1()
    }

    test("dynamic proxy") {
        val dp = new DynamicProxyTests
        dp.test1()
    }
}
