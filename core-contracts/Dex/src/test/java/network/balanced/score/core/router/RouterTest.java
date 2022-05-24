package network.balanced.score.core.router;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import com.iconloop.score.token.irc2.IRC2Basic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;
import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;


public class RouterTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static Account owner = sm.createAccount();
    private static Score routerScore;
    private static Account adminAccount = sm.createAccount();
    private static Account governanceScore = Account.newScoreAccount(0);

    @BeforeAll
    static void setup() throws Exception {
        routerScore = sm.deploy(owner, Dex.class, governanceScore.getAddress());
        routerScore.invoke(governanceScore, "setTimeOffset", BigInteger.valueOf(Context.getBlockTimestamp()));
    }

    @Test
    void testAdmin() {
        // initial constructor test
        assertEquals(
                routerScore.call("getAdmin"),
                governanceScore.getAddress()
        );
        Account testScore = governanceScore;
        routerScore.invoke(governanceScore, "setAdmin", adminAccount.getAddress());
        assertEquals(
                routerScore.call("getAdmin"),
                adminAccount.getAddress()
        );
    }

    @Test
    void testGovernance() {
        // initial constructor test
        assertEquals(
                routerScore.call("getGovernance"),
                governanceScore.getAddress()
        );
        Account governanceTestScore = Account.newScoreAccount(20);
        routerScore.invoke(owner, "setGovernance", governanceTestScore.getAddress());
        assertEquals(
                routerScore.call("getGovernance"),
                governanceTestScore.getAddress()
        );
    }


    @Test
    void testSicx(){
        Account sicxAccount = Account.newScoreAccount(1);
        Account testScore = governanceScore;
        routerScore.invoke(governanceScore, "setAdmin", adminAccount.getAddress());
        routerScore.invoke(
                adminAccount,
                "setSicx",
                sicxAccount.getAddress()
        );
        assertEquals(
                routerScore.call("getSicx"),
                sicxAccount.getAddress()
        );
    }


    @Test
    void testDeposit() throws Exception {
        Account account1 = Account.newScoreAccount(3);

        Score sicxScore = sm.deploy(owner, Token.class);
        Address[] addresses = new Address[1];
        addresses[0] = sicxScore.getAddress();

        account1.addBalance("TestToken", BigInteger.TEN);

        Score stakingScore = sm.deploy(owner, Token.class);
        // routerScore.invoke(governanceScore, "setStaking", stakingScore.getAddress());

        // routerScore.invoke(
        //         governanceScore,
        //         "setSicx",
        //         sicxScore.getAddress()
        // );

        // BigInteger minReceive = BigInteger.ZERO;
        // routerScore.invoke(adminAccount, "route", addresses, minReceive);

    }


    @Test
    void testIcxDeposit() throws Exception {
        Account icxLp = sm.createAccount(100);

        sm.transfer(icxLp, routerScore.getAddress(), BigInteger.valueOf(20).multiply(ICX));
    }

}
