package com.github.vzakharchenko.radius.radius.handlers.session;

import com.github.vzakharchenko.radius.test.AbstractRadiusTest;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;

import static com.github.vzakharchenko.radius.radius.handlers.session.AccountingSessionManager.ACCT_SESSION_ID;
import static com.github.vzakharchenko.radius.radius.handlers.session.AccountingSessionManager.ACCT_STATUS_TYPE;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class AccountingSessionManagerTest extends AbstractRadiusTest {
    private AccountingSessionManager accountingSessionManager;
    private AccountingRequest request;
    private RadiusEndpoint radiusEndpoint;

    @BeforeMethod
    public void beforeMethods() {
        request = new AccountingRequest(realDictionary, 0, new byte[16]);
        request.addAttribute("realm-radius", realmModel.getName());
        request.addAttribute("User-Name", userModel.getUsername());
        radiusEndpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");
        accountingSessionManager = new AccountingSessionManager(request, session, radiusEndpoint);

    }

    @Test
    public void testInit() {
        IAccountingSessionManager accountingSessionManager = this.accountingSessionManager.init();
        assertNotNull(accountingSessionManager);
    }

    @Test
    public void testUpdateContext() {
        this.accountingSessionManager.init().updateContext();
    }

    @Test
    public void testManageSessionCreate() {
        request.addAttribute(ACCT_SESSION_ID, "new Session");
        request.addAttribute(ACCT_STATUS_TYPE, "Start");
        this.accountingSessionManager.init().updateContext().manageSession();
        UserSessionProvider userSessionProvider1 = verify(userSessionProvider);
        userSessionProvider1.createUserSession(null, realmModel, userModel, "USER", "0.0.0.0",
                "radius", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    @Test
    public void testManageSessionNotExists() {
        request.addAttribute(ACCT_SESSION_ID, "new Session");
        request.addAttribute(ACCT_STATUS_TYPE, "Alive");
        assertFalse(this.accountingSessionManager.init()
                .updateContext().manageSession().isValidSession());
    }

    @Test
    public void testManageSessionLogout() {
        request.addAttribute(ACCT_SESSION_ID, "new Session");
        request.addAttribute(ACCT_STATUS_TYPE, "Alive");
        this.accountingSessionManager.init().updateContext().manageSession().logout();
    }

    @Test
    public void testManageSessionUnsupported() {
        request.addAttribute(ACCT_STATUS_TYPE, "Accounting-On");
        this.accountingSessionManager.init().updateContext().manageSession().logout();
    }

    @Test
    public void testManageSessionUpdateSession() {
        request.addAttribute(ACCT_SESSION_ID, RADIUS_SESSION_ID);
        request.addAttribute(ACCT_STATUS_TYPE, "Alive");
        this.accountingSessionManager.init().updateContext().manageSession();
    }

    @Test
    public void testManageSessionRemoveSession() {
        request.addAttribute(ACCT_SESSION_ID, RADIUS_SESSION_ID);
        request.addAttribute(ACCT_STATUS_TYPE, "Stop");
        IAccountingSessionManager manager = this.accountingSessionManager
                .init().updateContext().manageSession();
        verify(userSessionProvider).removeUserSession(realmModel,
                userSessionModel);
        assertFalse(manager.isValidSession());
    }

    @Test
    public void testManageSessionRemovenewSession() {
        request.addAttribute(ACCT_SESSION_ID, "new Session");
        request.addAttribute(ACCT_STATUS_TYPE, "Stop");
        this.accountingSessionManager.init().updateContext().manageSession();
        verify(userSessionProvider, never()).removeUserSession(realmModel,
                userSessionModel);
    }


}
