package server.dao;

import common.models.AutoBidSetting;
import common.models.BidTransaction;
import common.models.Product;
import common.models.Room;
import common.models.TransactionRecord;
import common.models.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionSystemDaoTest {

    // Fixed test data identifiers - reuse if exists, create if not
    private static final String TEST_BIDDER = "test_bidder";
    private static final String TEST_SELLER = "test_seller";
    private static final String TEST_SELLER_BID = "test_seller_bid";
    private static final String TEST_BIDDER1 = "test_bidder1";
    private static final String TEST_BIDDER2 = "test_bidder2";
    private static final String TEST_AUTO_USER = "test_auto_user";
    private static final String TEST_WALLET_USER = "test_wallet_user";
    
    private static final String TEST_PRODUCT_ID = "P_001";
    private static final String TEST_PRODUCT_BID = "P_002";
    private static final String TEST_PRODUCT_AUTO = "P_003";
    
    private static String testRoomId;
    private static String testRoomBidId;
    private static String testRoomAutoId;

    @BeforeAll
    public static void setup() {
        // Enable test mode to use database_test.db instead of myDatabase.db
        ConnectDatabase.setTestMode(true);
        // Initialize the test database connection
        ConnectDatabase.getConnection();
        // Initialize test database schema

        TestDataCleaner.initializeTestDatabase();
        
        // Initialize test data once for all tests
        initializeTestData();
    }


    private static void initializeTestData() {
        // Create test bidder if not exists
        String begintime = (LocalDateTime.now().plusMinutes(1)).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String endtime = (LocalDateTime.now().plusMinutes(15)).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        User bidder = UserDAO.login(TEST_BIDDER, "pass");
        if (bidder == null) {
            UserDAO.signUp(TEST_BIDDER, "pass", "BIDDER");
            WalletDAO.updateBalance(TEST_BIDDER, 1_000_000L);
        }
        
        // Create test seller if not exists
        User seller = UserDAO.login(TEST_SELLER, "pass");
        if (seller == null) {
            UserDAO.signUp(TEST_SELLER, "pass", "SELLER");
        }
        
        // Create test seller for bid if not exists
        User sellerBid = UserDAO.login(TEST_SELLER_BID, "pass");
        if (sellerBid == null) {
            UserDAO.signUp(TEST_SELLER_BID, "pass", "SELLER");
        }
        
        // Create test bidders for bid test if not exists
        User bidder1 = UserDAO.login(TEST_BIDDER1, "pass");
        if (bidder1 == null) {
            UserDAO.signUp(TEST_BIDDER1, "pass", "BIDDER");
            WalletDAO.updateBalance(TEST_BIDDER1, 1_000_000L);
        }
        
        User bidder2 = UserDAO.login(TEST_BIDDER2, "pass");
        if (bidder2 == null) {
            UserDAO.signUp(TEST_BIDDER2, "pass", "BIDDER");
            WalletDAO.updateBalance(TEST_BIDDER2, 500_000L);
        }
        
        // Create test auto user if not exists
        User autoUser = UserDAO.login(TEST_AUTO_USER, "pass");
        if (autoUser == null) {
            UserDAO.signUp(TEST_AUTO_USER, "pass", "BIDDER");
        }
        
        // Create test wallet user if not exists
        User walletUser = UserDAO.login(TEST_WALLET_USER, "pass");
        if (walletUser == null) {
            UserDAO.signUp(TEST_WALLET_USER, "pass", "BIDDER");
        }
        
        // Create test product if not exists
        ProductDAO.addProducts(TEST_PRODUCT_ID, "Test Product", "TYPE", "Details", TEST_SELLER);
        
        // Create test room if not exists
        Room existingRoom = RoomDAO.getRoomById(TEST_PRODUCT_ID);
        if (existingRoom == null) {
            Room room = new Room(null, "Test Room", TEST_PRODUCT_ID, TEST_SELLER, 100000L, begintime, endtime);
            Room created = RoomDAO.createRoom(room);
            if (created != null) {
                testRoomId = created.getRoomId();
                RoomDAO.updateRoomStatus(testRoomId, "ACTIVE");
            }
        } else {
            testRoomId = existingRoom.getRoomId();
            if (!"ACTIVE".equals(existingRoom.getStatus())) {
                RoomDAO.updateRoomStatus(testRoomId, "ACTIVE");
            }
        }
        
        // Create test product for bid if not exists
        ProductDAO.addProducts(TEST_PRODUCT_BID, "Bid Product", "TYPE", "Bid Details", TEST_SELLER_BID);
        
        // Create test room for bid if not exists
        Room existingRoomBid = RoomDAO.getRoomById(TEST_PRODUCT_BID);
        if (existingRoomBid == null) {
            Room room = new Room(null, "Bid Test Room", TEST_PRODUCT_BID, TEST_SELLER_BID, 50000L, begintime, endtime);
            Room created = RoomDAO.createRoom(room);
            if (created != null) {
                testRoomBidId = created.getRoomId();
                RoomDAO.updateRoomStatus(testRoomBidId, "ACTIVE");
            }
        } else {
            testRoomBidId = existingRoomBid.getRoomId();
            if (!"ACTIVE".equals(existingRoomBid.getStatus())) {
                RoomDAO.updateRoomStatus(testRoomBidId, "ACTIVE");
            }
        }
        
        // Create test product for auto if not exists
        ProductDAO.addProducts(TEST_PRODUCT_AUTO, "Auto Product", "TYPE", "Auto Details", TEST_SELLER);
        
        // Create test room for auto if not exists
        Room existingRoomAuto = RoomDAO.getRoomById(TEST_PRODUCT_AUTO);
        if (existingRoomAuto == null) {
            Room room = new Room(null, "Auto Test Room", TEST_PRODUCT_AUTO, TEST_SELLER, 10000L, begintime, endtime);
            Room created = RoomDAO.createRoom(room);
            if (created != null) {
                testRoomAutoId = created.getRoomId();
            }
        } else {
            testRoomAutoId = existingRoomAuto.getRoomId();
        }
    }

    @AfterAll
    public static void teardown() {
        ConnectDatabase.closeConnection();
        TestDataCleaner.clearTestData();
        // Reset to production mode (for safety, in case other code uses this)
        ConnectDatabase.setTestMode(false);
    }
    @Test
    public void testSignUpLoginGetRoleAndSessionLogout() {
        User logged = UserDAO.login(TEST_BIDDER, "pass");
        assertNotNull(logged, "login should succeed with correct credentials");
        assertEquals(TEST_BIDDER, logged.getUsername());

        String role = UserDAO.getUserRole(TEST_BIDDER);
        assertEquals("BIDDER", role);

        // Session logout behavior
        client.controllers.Session s1 = client.controllers.Session.getInstance();
        s1.setCurrentUser(logged);
        client.controllers.Session sBefore = s1;

        s1.closeConnection();

        client.controllers.Session s2 = client.controllers.Session.getInstance();
        assertNotNull(s2, "A new Session instance should be created after close");
        assertNotSame(sBefore, s2, "Session instance should be reset after closeConnection");
        assertNull(s2.getCurrentUser(), "New session should not carry previous currentUser");
    }

    @Test
    public void testCreateProductCreateRoomAndConfirm() {
        assertTrue(ProductDAO.isProductValid(TEST_PRODUCT_ID), "Product should exist and be valid (isSold==0)");

        // Get existing room
        Room fetched = RoomDAO.getRoomById(testRoomId);
        assertNotNull(fetched);
        assertEquals("ACTIVE", fetched.getStatus());
        assertEquals(TEST_PRODUCT_ID, fetched.getProductId());
    }

    @Test
    public void testPlaceBidAndHistoryAndParticipantCount() {
        Room active = RoomDAO.getRoomById(testRoomBidId);
        assertNotNull(active);
        assertEquals("ACTIVE", active.getStatus());

        long bidAmount = active.getStartingPrice() + 10000L;
        long newBalanceForBidder1 = 1_000_000L - bidAmount;

        // Place bid if not already placed by this bidder
        List<BidTransaction> existingBids = BidDAO.getBidHistory(active);
        boolean alreadyBid = existingBids.stream().anyMatch(b -> TEST_BIDDER1.equals(b.getBidderUsername()));
        
        if (!alreadyBid) {
            boolean placed = BidDAO.placeBid(active, null, TEST_BIDDER1, bidAmount, 0L, newBalanceForBidder1);
            assertTrue(placed, "placeBid should succeed for valid inputs");
        }

        BidTransaction latest = BidDAO.getLatestBid(active.getRoomId());
        assertNotNull(latest);
        assertEquals(TEST_BIDDER1, latest.getBidderUsername());
        assertEquals(bidAmount, latest.getBidAmount());

        long currentPrice = BidDAO.getCurrentPrice(active.getRoomId());
        assertEquals(bidAmount, currentPrice);

        int participants = BidDAO.getParticipantCount(active.getRoomId());
        assertTrue(participants >= 1);

        List<BidTransaction> history = BidDAO.getBidHistory(active);
        assertNotNull(history);
        assertTrue(history.stream().anyMatch(b -> TEST_BIDDER1.equals(b.getBidderUsername()) && b.getBidAmount() == bidAmount));
    }

    @Test
    public void testAutoBidSaveGetCheckRemove() {
        // Check if auto bid already exists
        List<AutoBidSetting> existingBids = AutoBidDAO.getAutoBidders(testRoomAutoId);
        boolean autoBidExists = existingBids.stream().anyMatch(a -> TEST_AUTO_USER.equals(a.getUsername()));
        
        long maxPrice = 200000L;
        
        if (!autoBidExists) {
            long now = System.currentTimeMillis();
            boolean saved = AutoBidDAO.saveAutoBid(testRoomAutoId, TEST_AUTO_USER, maxPrice, now);
            assertTrue(saved);
        }

        List<AutoBidSetting> list = AutoBidDAO.getAutoBidders(testRoomAutoId);
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(a -> a.getUsername().equals(TEST_AUTO_USER) && a.getMaxPrice() == maxPrice));

        long fetchedMax = AutoBidDAO.checkAutoBidStatus(testRoomAutoId, TEST_AUTO_USER);
        assertEquals(maxPrice, fetchedMax);
    }

    @Test
    public void testWalletProcessDepositAndWithdraw() {
        // Get current balance
        long currentBalance = WalletDAO.getBalance(TEST_WALLET_USER);
        
        // Reset to known state if needed
        if (currentBalance != 300000L) {
            WalletDAO.updateBalance(TEST_WALLET_USER, 0L);
            
            boolean depositOk = WalletDAO.processTransaction(TEST_WALLET_USER, 300000L, "DEPOSIT", "TEST_METHOD");
            assertTrue(depositOk);
        }

        long bal = WalletDAO.getBalance(TEST_WALLET_USER);
        assertEquals(300000L, bal);

        // Check if withdrawal already done
        List<TransactionRecord> history = WalletDAO.getHistory(TEST_WALLET_USER);
        boolean withdrawalDone = history.stream().anyMatch(t -> "WITHDRAW".equals(t.getType()) && t.getAmount().equals(100000L));
        
        if (!withdrawalDone) {
            boolean withdrawOk = WalletDAO.processTransaction(TEST_WALLET_USER, 100000L, "WITHDRAW", "ATM");
            assertTrue(withdrawOk);
        }

        long balAfter = WalletDAO.getBalance(TEST_WALLET_USER);
        assertEquals(200000L, balAfter);

        // withdraw more than balance should fail
        boolean overWithdraw = WalletDAO.processTransaction(TEST_WALLET_USER, 500000L, "WITHDRAW", "ATM");
        assertFalse(overWithdraw);

        List<TransactionRecord> finalHistory = WalletDAO.getHistory(TEST_WALLET_USER);
        assertNotNull(finalHistory);
        assertTrue(finalHistory.size() >= 1);
    }
}


