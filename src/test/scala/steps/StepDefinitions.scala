package steps
import io.cucumber.scala.{EN, ScalaDsl}

class StepDefinitions extends ScalaDsl with EN {
  var concatenatedString: String = ""

  Given("Setup is correct") { () =>
    true
  }

  When("I concatenate {string} and {string}") {
    (string1: String, string2: String) =>
      concatenatedString = string1 + string2

  }

  Then("The result should be {string}") { (string: String) =>
    assert(concatenatedString == string)
  }
}
