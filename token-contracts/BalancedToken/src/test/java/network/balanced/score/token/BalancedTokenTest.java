package network.balanced.score.token;

import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.ServiceManager.Block;
import com.iconloop.score.test.TestBase;

import network.balanced.score.token.util.Mathematics;
import score.Context;

public class BalancedTokenTest extends TestBase {

	private static ServiceManager sm = getServiceManager();
	private static Account owner = sm.createAccount();

	//private BigInteger decimals = BigInteger.TEN;
	private static BigInteger totalSupply = BigInteger.valueOf(50000000000L);

	private Score balancedToken;
	private Score mockDexScore;
	private Account accountAddressProvider = sm.createAccount();
	private Account accountGovernance = sm.createAccount();
	private Account adminAccount = sm.createAccount();

	private Account dexAccount = sm.createAccount();
	private Account oracleAccount = sm.createAccount();

	@BeforeAll
	public static void init() {
		owner.addBalance(BalancedToken.SYMBOL_NAME, totalSupply);
	}

	@BeforeEach
	public void setup() throws Exception {
		balancedToken = sm.deploy(owner, BalancedToken.class, accountGovernance.getAddress(), false);
		mockDexScore = sm.deploy(owner, MockDexScore.class);
	}

	@Test
	void shouldAllowToSetOracleName() {
		String newOracleName = "icon oracle";
		String oracleName = (String)balancedToken.call("getOracleName");
		
		assertNotNull(oracleName);
		assertEquals(BalancedToken.DEFAULT_ORACLE_NAME, oracleName);

		balancedToken.invoke(accountGovernance, "setOracleName", newOracleName);

		oracleName = (String)balancedToken.call("getOracleName");
		assertNotNull(oracleName);
		assertEquals(newOracleName, oracleName);
	}

	@Test
	void shouldNotAllowToSetOracleName() {
		AssertionError error = Assertions.assertThrows(AssertionError.class, 
				() -> balancedToken.invoke(accountAddressProvider, "setOracleName", "failed assigment"));
		assertEquals(
				"Reverted(0): BALN: SenderNotGovernanceError: (sender)"+accountAddressProvider.getAddress()+" (governance)"+accountGovernance.getAddress(),
				error.getMessage());
	}

	@Test
	void ShouldSetAndGetActualPriceInLoop() {
		try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

			balancedToken.invoke(accountGovernance, "setDex", dexAccount.getAddress());
			balancedToken.invoke(accountGovernance, "setOracle", oracleAccount.getAddress());

			theMock
			.when(() -> Context.getBlockTimestamp())
			.thenReturn(34834316634l);

			BigInteger balnPrice = BigInteger.TEN.subtract(BigInteger.ONE);
			theMock
			.when(() -> Context.call(BigInteger.class, dexAccount.getAddress(), "getBalnPrice"))
			.thenReturn(balnPrice);

			BigInteger rate = BigInteger.valueOf(3500000000000000000l);
			theMock
			.when(() -> Context.call(Map.class, oracleAccount.getAddress(),"get_reference_data","USD", "ICX"))
			.thenReturn(Map.of("rate", rate));

			//the icon test fwk has an issue where it does not allow to return
			//values from write method if they actually writes any value.
			//first call, it will update last price
			balancedToken.invoke(accountAddressProvider, "priceInLoop");
			//second call it will return the last price only
			BigInteger price = (BigInteger) balancedToken.call("priceInLoop");
			assertNotNull(price);
			BigInteger expectedPrice = rate.multiply(balnPrice).divide(Constants.EXA);
			assertEquals(expectedPrice, price);
		}
	}

	@Test
	void ShouldGetLastPriceInLoop() {
		try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

			balancedToken.invoke(accountGovernance, "setDex", dexAccount.getAddress());
			balancedToken.invoke(accountGovernance, "setOracle", oracleAccount.getAddress());

			theMock
			.when(() -> Context.getBlockTimestamp())
			.thenReturn(34834316634l);

			BigInteger balnPrice = BigInteger.TEN.subtract(BigInteger.ONE);
			theMock
			.when(() -> Context.call(BigInteger.class, dexAccount.getAddress(), "getBalnPrice"))
			.thenReturn(balnPrice);

			BigInteger rate = BigInteger.valueOf(3500000000000000000l);
			theMock
			.when(() -> Context.call(Map.class, oracleAccount.getAddress(),"get_reference_data","USD", "ICX"))
			.thenReturn(Map.of("rate", rate));

			BigInteger price = (BigInteger) balancedToken.call("lastPriceInLoop");
			assertNotNull(price);
			BigInteger expectedPrice = rate.multiply(balnPrice).divide(Constants.EXA);
			assertEquals(expectedPrice, price);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldMint() {
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());
		BigInteger amount = BigInteger.valueOf(10000l);
		balancedToken.invoke(adminAccount, "mint", amount, "init gold".getBytes());
		Map<String, BigInteger>  balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", adminAccount.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(amount , balanceDetails.get("Total balance"));
		assertEquals(amount , balanceDetails.get("Available balance"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldMintToSomeone() {
		Account accountToMint = sm.createAccount();
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());

		BigInteger amount = BigInteger.valueOf(10000l);
		balancedToken.invoke(adminAccount, "mintTo", accountToMint.getAddress(), amount, "init gold".getBytes());

		Map<String, BigInteger>  balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", accountToMint.getAddress());
		assertNotNull(balanceDetails);
		assertEquals(amount , balanceDetails.get("Total balance"));
		assertEquals(amount , balanceDetails.get("Available balance"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldStake() {
		//set admin
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());

		//mint some tokens
		BigInteger amountToMint =  BigInteger.valueOf(10000l).multiply(Mathematics.pow(BigInteger.TEN, 18));
		balancedToken.invoke(adminAccount, "mint", amountToMint, "init gold".getBytes());

		//enable staking
		balancedToken.invoke(accountGovernance, "toggleStakingEnabled");
		BigInteger stakedAmount = amountToMint.divide(TWO);
		//stake
		balancedToken.invoke(adminAccount, "stake", stakedAmount);

		Map<String, BigInteger>  balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", adminAccount.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(amountToMint , balanceDetails.get("Total balance"));
		assertEquals(amountToMint.divide(TWO) , balanceDetails.get("Available balance"));
		assertEquals(amountToMint.divide(TWO) , balanceDetails.get("Staked balance"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldTransfer() {
		//set admin
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());

		//mint some tokens
		BigInteger amountToMint =  BigInteger.valueOf(10000l).multiply(Mathematics.pow(BigInteger.TEN, 18));
		balancedToken.invoke(adminAccount, "mint", amountToMint, "init gold".getBytes());

		//enable staking
		balancedToken.invoke(accountGovernance, "toggleStakingEnabled");
		BigInteger stakedAmount = amountToMint.divide(TWO);
		//Stake
		balancedToken.invoke(adminAccount, "stake", stakedAmount);

		//transfer
		Account user = sm.createAccount();
		BigInteger amountToTransfer = amountToMint.divide(TWO).divide(TWO);
		balancedToken.invoke(adminAccount, "transfer", user.getAddress(), amountToTransfer, "tacos".getBytes());

		Map<String, BigInteger>  balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", user.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(amountToTransfer , balanceDetails.get("Total balance"));
		assertEquals(amountToTransfer , balanceDetails.get("Available balance"));

	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldBurn() {
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());
		BigInteger amount = BigInteger.valueOf(10000l);
		balancedToken.invoke(adminAccount, "mint", amount, "init gold".getBytes());
		Map<String, BigInteger>  balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", adminAccount.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(amount , balanceDetails.get("Total balance"));
		assertEquals(amount , balanceDetails.get("Available balance"));

		BigInteger balanceToBurn = amount.divide(TWO);
		balancedToken.invoke(adminAccount, "burn", balanceToBurn);

		balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", adminAccount.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(balanceToBurn , balanceDetails.get("Total balance"));
		assertEquals(balanceToBurn , balanceDetails.get("Available balance"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldBurnFrom() {
		Account accountToMint = sm.createAccount();
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());
		BigInteger amount = BigInteger.valueOf(10000l);
		balancedToken.invoke(adminAccount, "mintTo", accountToMint.getAddress(), amount, "init gold".getBytes());
		Map<String, BigInteger>  balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", accountToMint.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(amount , balanceDetails.get("Total balance"));
		assertEquals(amount , balanceDetails.get("Available balance"));

		BigInteger balanceToBurn = amount.divide(TWO);
		balancedToken.invoke(adminAccount, "burnFrom", accountToMint.getAddress(), balanceToBurn);

		balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", accountToMint.getAddress());

		assertNotNull(balanceDetails);
		assertEquals(balanceToBurn , balanceDetails.get("Total balance"));
		assertEquals(balanceToBurn , balanceDetails.get("Available balance"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void ShouldGetStakedBalanceOfAt() {
		boolean snapshotEnabled = (boolean)balancedToken.call("getSnapshotEnabled");
		if(!snapshotEnabled) {
			//enable snapshot of staked balances
			balancedToken.invoke(owner, "toggleEnableSnapshot");
		}

		//set admin
		balancedToken.invoke(accountGovernance, "setAdmin", adminAccount.getAddress());

		//mint some tokens
		BigInteger amountToMint =  BigInteger.valueOf(10000l).multiply(Mathematics.pow(BigInteger.TEN, 18));
		balancedToken.invoke(adminAccount, "mintTo", owner.getAddress(), amountToMint, "init gold".getBytes());

		//enable staking
		balancedToken.invoke(accountGovernance, "toggleStakingEnabled");

		balancedToken.invoke(accountGovernance, "setDex", mockDexScore.getAddress());
		BigInteger stakedAmount = amountToMint.divide(TWO);

		//stake
		balancedToken.invoke(owner, "stake", stakedAmount);

		balancedToken.invoke(owner, "setTimeOffset");

		//move the day, so there are 2 snapshots
		sm.getBlock().increase(100000000000l);

		//stake twice
		balancedToken.invoke(owner, "stake", stakedAmount.divide(TWO));

		//improve this day calculation
		BigInteger day = BigInteger.valueOf(Context.getBlockTimestamp()).subtract(BigInteger.valueOf(2000000214314191l)).divide(Constants.DAY_TO_MICROSECOND);
		//ask for staked balance of snapshot 1
		BigInteger stakedBalance = (BigInteger)balancedToken.call("stakedBalanceOfAt", owner.getAddress(), day);

		assertNotNull(stakedBalance);
		assertEquals(stakedAmount, stakedBalance);

		Map<String, BigInteger> balanceDetails = (Map<String, BigInteger>)balancedToken.call("detailsBalanceOf", owner.getAddress());
		assertNotNull(balanceDetails);

		assertEquals(amountToMint , balanceDetails.get("Total balance"));
		assertEquals(amountToMint.divide(TWO), balanceDetails.get("Available balance"));
		assertEquals(stakedAmount.divide(TWO), balanceDetails.get("Staked balance"));
		assertEquals(stakedAmount.divide(TWO), balanceDetails.get("Unstaking balance"));
	}

}
