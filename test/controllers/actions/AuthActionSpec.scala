package controllers.actions

import play.api.mvc.ControllerComponents
import play.api.test.StubControllerComponentsFactory
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController
import utils.BaseSpec

class AuthActionSpec  extends BaseSpec{

  "AuthAction" must {

    "return None" when {
      "authenticated" in {

        object Harness extends BackendBaseController with StubControllerComponentsFactory {
          override protected def controllerComponents: ControllerComponents = stubControllerComponents()


        }
      }
    }

    "return forbidden" when {
      "authenticated" in {

      }
    }
  }
}
