package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.TestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.org.junit.After;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static com.dotcms.repackage.org.junit.Assert.assertEquals;
import static com.dotcms.repackage.org.junit.Assert.assertNotSame;

/**
 * jUnit test used to verify the results of calling the actionlets provided
 * out of the box in dotCMS.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 *
 */
public class SetSessionAttributeActionletTest extends TestBase {

    private HttpServletRequest request;
    private String serverName;
    private Integer serverPort;
    String ruleId;

    public SetSessionAttributeActionletTest(){
        request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        ruleId = "";
    }

    @Test
    public void testFireOnEveryRequest() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.EVERY_REQUEST, firstTime);

        makeRequest(
                "http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis(),
                "JSESSIONID=" + request.getSession().getId());

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest(
                "http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis(),
                "JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(secondTime, secondTimeRequest);

        assertNotSame(firstTimeRequest, secondTimeRequest);

        deleteRule();
    }

    @Test
    public void testFireOnEveryPage() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.EVERY_PAGE, firstTime);

        makeRequest("http://" + serverName + ":" + serverPort,
                "JSESSIONID=" + request.getSession().getId());

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest(
                "http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis(),
                "JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, secondTimeRequest);

        deleteRule();
    }

    @Test
    public void testFireOnOncePerVisit() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.ONCE_PER_VISIT, firstTime);

        URLConnection conn = makeRequest("http://" + serverName + ":" + serverPort,
                "JSESSIONID=" + request.getSession().getId());

        String oncePerVisitCookie = getCookie(conn, com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE);

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest("http://" + serverName + ":" + serverPort,
                oncePerVisitCookie + ";JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, secondTimeRequest);

        deleteRule();
    }

    @Test
    public void testFireOnOncePerVisitor() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.ONCE_PER_VISITOR, firstTime);

        URLConnection conn = makeRequest("http://" + serverName + ":" + serverPort,
                "JSESSIONID=" + request.getSession().getId());

        String longLivedCookie = getCookie(conn, com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest("http://" + serverName + ":" + serverPort,
                longLivedCookie + ";JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, secondTimeRequest);

        deleteRule();
    }

    private URLConnection makeRequest(String urlStr, String cookie) throws IOException {
        URL url = new URL(urlStr);
        URLConnection con = url.openConnection();

        if (cookie != null) {
            con.setRequestProperty("Cookie", cookie);
        }

        con.connect();
        con.getInputStream();
        return con;
    }

    private String getCookie(URLConnection conn, String cookieName) {

        String longLivedCookie = null;
        Map<String, List<String>> headerFields = conn.getHeaderFields();

        Set<String> headerFieldsSet = headerFields.keySet();
        Iterator<String> hearerFieldsIter = headerFieldsSet.iterator();

        while (hearerFieldsIter.hasNext()) {

            String headerFieldKey = hearerFieldsIter.next();

            if ("Set-Cookie".equalsIgnoreCase(headerFieldKey)) {

                List<String> headerFieldValue = headerFields.get(headerFieldKey);

                for (String headerValue : headerFieldValue) {
                    String[] fields = headerValue.split(";");
                    String cookieValue = fields[0];
                    if (cookieValue.contains(cookieName)) {
                        longLivedCookie = cookieValue;
                        break;
                    }
                }
            }

            if (longLivedCookie != null)
                break;

        }
        return longLivedCookie;
    }

    private void updateRuleActionParam(String sessionValue) throws Exception{
        RulesAPI rulesAPI = APILocator.getRulesAPI();
        User user = APILocator.getUserAPI().getSystemUser();

        Rule rule = rulesAPI.getRuleById(ruleId, user, false);
        //As we created the rule, we are sure there is only one in there.
        RuleAction ruleAction = rule.getRuleActions().get(0);

        //Updating just the sessionValue RuleActionParameter.
        List<ParameterModel> parameterModels = Lists.newArrayList(ruleAction.getParameters().values());
        for (ParameterModel parameterModel : parameterModels) {
            if (parameterModel.getKey().equals("sessionValue")){
                parameterModel.setValue(sessionValue);
            }
        }

        //Saving updated Action.
        rulesAPI.saveRuleAction(ruleAction, user, false);
    }

    private void createRule(Rule.FireOn fireOn, String sessionValue) throws Exception {
        RulesAPI rulesAPI = APILocator.getRulesAPI();
        User user = APILocator.getUserAPI().getSystemUser();

        HostAPI hostAPI = APILocator.getHostAPI();
        Host defaultHost = hostAPI.findDefaultHost(user, false);

        // Create Rule
        Rule rule = new Rule();
        rule.setName(fireOn.name() + "Rule");
        rule.setHost(defaultHost.getIdentifier());
        rule.setEnabled(true);
        rule.setFireOn(fireOn);

        rulesAPI.saveRule(rule, user, false);

        ruleId = rule.getId();



        RuleAction action = new RuleAction();
        action.setActionlet(SetSessionAttributeActionlet.class.getSimpleName());
        action.setRuleId(rule.getId());
        action.setName("SetSessionAttributeActionlet");

        ParameterModel timeKeyParam = new ParameterModel();
        timeKeyParam.setOwnerId(action.getId());
        timeKeyParam.setKey("sessionKey");
        timeKeyParam.setValue("time");

        ParameterModel timeValueParam = new ParameterModel();
        timeValueParam.setOwnerId(action.getId());
        timeValueParam.setKey("sessionValue");
        timeValueParam.setValue(sessionValue);

        List<ParameterModel> params = new ArrayList<>();
        params.add(timeKeyParam);
        params.add(timeValueParam);

        action.setParameters(params);

        rulesAPI.saveRuleAction(action, user, false);
    }

    @After
    public void deleteRule() throws DotDataException, DotSecurityException {
        if (ruleId != null) {
            APILocator.getRulesAPI().deleteRule(
                    APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
            ruleId = null;
        }
    }

}
