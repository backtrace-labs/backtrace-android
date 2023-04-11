package backtraceio.coroner.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static backtraceio.coroner.utils.ResourceUtils.RESPONSE_RX_FILTER_CORONER_JSON;
import static backtraceio.coroner.utils.ResourceUtils.readResourceFile;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerResponseGroup;


public class GsonWrapperTest {

    private Object getResponseGroupAttributeValue(Object attribute) {
        return ((List<?>) attribute).get(0);
    }

    @Test
    public void deserializeApiResponse() throws IOException{
        // GIVEN
        final String json = readResourceFile(RESPONSE_RX_FILTER_CORONER_JSON);

        // WHEN
        final CoronerApiResponse result = GsonWrapper.fromJson(json, CoronerApiResponse.class);

        // THEN
        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getResponse());
        assertEquals(1, result.getResponse().getResultsNumber());

        CoronerResponseGroup responseGroup = result.getResponse().values.get(0);
        assertEquals("Invalid index of selected element!", getResponseGroupAttributeValue(responseGroup.getAttribute(0)));
        assertEquals("{\"frame\":[\"backtraceio.backtraceio.MainActivity.handledException\",\"androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick\",\"android.view.View.performClick\",\"android.view.View.performClickInternal\",\"android.view.View.access$3600\",\"android.view.View$PerformClick.run\",\"androidx.test.espresso.base.Interrogator.loopAndInterrogate\",\"androidx.test.espresso.base.UiControllerImpl.loopUntil\",\"androidx.test.espresso.base.UiControllerImpl.injectMotionEvent\",\"androidx.test.espresso.action.MotionEvents.sendUp\",\"androidx.test.espresso.action.Tap.sendSingleTap\",\"androidx.test.espresso.action.Tap.-$$Nest$smsendSingleTap\",\"androidx.test.espresso.action.Tap$1.sendTap\",\"androidx.test.espresso.action.GeneralClickAction.perform\",\"androidx.test.espresso.ViewInteraction$SingleExecutionViewAction.perform\",\"androidx.test.espresso.ViewInteraction.doPerform\",\"androidx.test.espresso.ViewInteraction.-$$Nest$mdoPerform\",\"androidx.test.espresso.ViewInteraction$1.call\",\"java.util.concurrent.FutureTask.run\",\"android.os.Handler.handleCallback\",\"android.os.Handler.dispatchMessage\",\"android.os.Looper.loop\",\"android.app.ActivityThread.main\",\"java.lang.reflect.Method.invoke\",\"com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run\",\"com.android.internal.os.ZygoteInit.main\"]}", getResponseGroupAttributeValue(responseGroup.getAttribute(1)));
        assertEquals("e4c57699-0dc9-35e2-b4a0-2ffff1925ca7", getResponseGroupAttributeValue(responseGroup.getAttribute(2)));
        assertEquals("java.lang.IndexOutOfBoundsException", getResponseGroupAttributeValue(responseGroup.getAttribute(3)));
    }
}
