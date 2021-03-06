package uk.gov.dvsa.motr.web.render;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.dvsa.motr.web.cookie.UserCookiePreferences;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class HandlebarsTemplateEngineTest {

    UserCookiePreferences userCookiePreferences = new UserCookiePreferences();

    @Before
    public void setup() {
        this.userCookiePreferences.setShouldUseAnalytics(false);
    }

    @DataProvider
    public static Object[][] baseUrl() {
        // @formatter:off
        return new Object[][]{
                {"https://assets/", "/vrm", "https://assets/vrm"},
                {"http://assets//", "/vrm", "http://assets/vrm"},
                {"http://assets//", "vrm", "http://assets/vrm"},
                {"/", "/sub/vrm", "/sub/vrm"},
                {"http://assets", "/sub/vrm", "http://assets/sub/vrm"},
        };
    }
    // @formatter:on

    @UseDataProvider("baseUrl")
    @Test
    public void assetHelperBuildsPathsCorrectly(String assetsPath, String path, String result) {

        HandlebarsTemplateEngine instance =
                new HandlebarsTemplateEngine(assetsPath, "12345", "baseUrl", "releaseVersion", this.userCookiePreferences);

        Map<String, String> context = new HashMap<>();
        context.put("path", path);

        String actualOutput = instance.render("test-assetshelper", context);
        String expectedOutput = result + "?v=12345";

        assertEquals("Received output is wrong", expectedOutput, actualOutput);
    }

    @Test
    public void testEngineCorrectlyFillsContextKey() {

        HandlebarsTemplateEngine instance =
                new HandlebarsTemplateEngine("/", "12345", "baseUrl", "releaseVersion", this.userCookiePreferences);

        Map<String, String> context = new HashMap<>();
        context.put("key", "SomeValueOfTheKey");

        String actualOutput = instance.render("test-contextkey", context);

        assertEquals("Received output is wrong", "SomeValueOfTheKey", actualOutput);
    }

    @UseDataProvider("baseUrl")
    @Test
    public void urlHelperCorrectlyBuildsPathsCorrectly(String basePath, String path, String expectedOutput) {

        HandlebarsTemplateEngine instance =
                new HandlebarsTemplateEngine("someassetPath", "12345", basePath, "releaseVersion", this.userCookiePreferences);

        Map<String, String> context = new HashMap<>();
        context.put("path", path);

        String actualOutput = instance.render("test-urlhelper", context);

        assertEquals("Received output is wrong", expectedOutput, actualOutput);
    }
}
