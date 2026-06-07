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
        BidDAO.placeBid(active, null, TEST_BIDDER1, bidAmount, 0L, newBalanceForBidder1);

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
        //boolean withdrawalDone = history.stream().anyMatch(t -> "WITHDRAW".equals(t.getType()) && t.getAmount().equals(100000L));
        
        //if (!withdrawalDone) {
            boolean withdrawOk = WalletDAO.processTransaction(TEST_WALLET_USER, 100000L, "WITHDRAW", "ATM");
            assertTrue(withdrawOk);
        //}

        long balAfter = WalletDAO.getBalance(TEST_WALLET_USER);
        assertEquals(200000L, balAfter);

        // withdraw more than balance should fail
        boolean overWithdraw = WalletDAO.processTransaction(TEST_WALLET_USER, 500000L, "WITHDRAW", "ATM");
        assertFalse(overWithdraw);

        List<TransactionRecord> finalHistory = WalletDAO.getHistory(TEST_WALLET_USER);
        assertNotNull(finalHistory);
        assertTrue(finalHistory.size() >= 1);
    }


    @Test
    public void testBiddingCompetitionMultipleBidders() {
        Room room = RoomDAO.getRoomById(testRoomBidId);
        assertNotNull(room, "Room should exist");
        assertEquals("ACTIVE", room.getStatus());

        long startingPrice = room.getStartingPrice();
        long bidStep = Room.calculateDefaultBidStep(startingPrice);

        // Bidder 1: Places first bid
        long bid1Amount = startingPrice + bidStep;
        long bidder1NewBalance = 1_000_000L - bid1Amount;

        List<BidTransaction> existingBids1 = BidDAO.getBidHistory(room);
        boolean bidder1AlreadyBid = existingBids1.stream()
                .anyMatch(b -> TEST_BIDDER1.equals(b.getBidderUsername()) && b.getBidAmount() == bid1Amount);

        if (!bidder1AlreadyBid) {
            boolean bid1Placed = BidDAO.placeBid(room, null, TEST_BIDDER1, bid1Amount, 0L, bidder1NewBalance);
            assertTrue(bid1Placed, "Bidder1 should successfully place first bid");
        }

        // Verify Bidder1 is current winner
        BidTransaction latestBid1 = BidDAO.getLatestBid(room.getRoomId());
        assertNotNull(latestBid1);
        assertEquals(TEST_BIDDER1, latestBid1.getBidderUsername());
        assertEquals(bid1Amount, latestBid1.getBidAmount());

        // Verify current price updated
        long currentPrice1 = BidDAO.getCurrentPrice(room.getRoomId());
        assertEquals(bid1Amount, currentPrice1);

        // Bidder 2: Outbids Bidder1
        long bid2Amount = bid1Amount + bidStep;
        long bidder2NewBalance = 500_000L - bid2Amount;

        List<BidTransaction> existingBids2 = BidDAO.getBidHistory(room);
        boolean bidder2AlreadyBid = existingBids2.stream()
                .anyMatch(b -> TEST_BIDDER2.equals(b.getBidderUsername()) && b.getBidAmount() == bid2Amount);

        if (!bidder2AlreadyBid) {
            boolean bid2Placed = BidDAO.placeBid(room, TEST_BIDDER1, TEST_BIDDER2, bid2Amount, bidder1NewBalance, bidder2NewBalance);
            assertTrue(bid2Placed, "Bidder2 should successfully outbid Bidder1");
        }

        // Verify Bidder2 is now the winner
        BidTransaction latestBid2 = BidDAO.getLatestBid(room.getRoomId());
        assertNotNull(latestBid2);
        assertEquals(TEST_BIDDER2, latestBid2.getBidderUsername());
        assertEquals(bid2Amount, latestBid2.getBidAmount());

        // Verify current price updated to Bidder2's bid
        long currentPrice2 = BidDAO.getCurrentPrice(room.getRoomId());
        assertEquals(bid2Amount, currentPrice2);

        // Verify participant count increased
        int participants = BidDAO.getParticipantCount(room.getRoomId());
        assertTrue(participants >= 2, "Should have at least 2 participants");

        // Verify full bid history
        List<BidTransaction> finalHistory = BidDAO.getBidHistory(room);
        assertNotNull(finalHistory);
        assertTrue(finalHistory.stream().anyMatch(b -> TEST_BIDDER1.equals(b.getBidderUsername())),
                "Bidder1 should be in history");
        assertTrue(finalHistory.stream().anyMatch(b -> TEST_BIDDER2.equals(b.getBidderUsername())),
                "Bidder2 should be in history");
    }


    @Test
    public void testAutoBidExecutionFlow() {
        Room room = RoomDAO.getRoomById(testRoomAutoId);
        assertNotNull(room, "Auto-bid test room should exist");

        long initialMaxPrice = 300000L;
        long now = System.currentTimeMillis();

        // Step 1: Save auto-bid setting
        List<AutoBidSetting> existingAutoBids = AutoBidDAO.getAutoBidders(testRoomAutoId);
        boolean autoUserAlreadySet = existingAutoBids.stream()
                .anyMatch(a -> TEST_AUTO_USER.equals(a.getUsername()));

        if (!autoUserAlreadySet) {
            boolean saved = AutoBidDAO.saveAutoBid(testRoomAutoId, TEST_AUTO_USER, initialMaxPrice, now);
            assertTrue(saved, "Auto-bid should be saved successfully");
        }

        // Step 2: Verify auto-bid setting is stored
        long fetchedMaxPrice = AutoBidDAO.checkAutoBidStatus(testRoomAutoId, TEST_AUTO_USER);
        assertEquals(initialMaxPrice, fetchedMaxPrice, "Auto-bid max price should match saved value");

        // Step 3: Verify auto-bidder in list
        List<AutoBidSetting> autoBidders = AutoBidDAO.getAutoBidders(testRoomAutoId);
        assertNotNull(autoBidders);
        assertTrue(autoBidders.stream().anyMatch(a ->
                TEST_AUTO_USER.equals(a.getUsername()) && a.getMaxPrice() == initialMaxPrice),
                "Auto-bidder should be in the list with correct max price");

        // Step 4: Update auto-bid max price (simulate user changing limit)
        long updatedMaxPrice = 400000L;
        boolean updated = AutoBidDAO.saveAutoBid(testRoomAutoId, TEST_AUTO_USER, updatedMaxPrice, now);
        assertTrue(updated, "Auto-bid max price should be updateable");

        // Step 5: Verify updated max price
        long newFetchedMaxPrice = AutoBidDAO.checkAutoBidStatus(testRoomAutoId, TEST_AUTO_USER);
        assertEquals(updatedMaxPrice, newFetchedMaxPrice, "Auto-bid should reflect updated max price");

        // Step 6: Verify update is reflected in list
        List<AutoBidSetting> updatedAutoBidders = AutoBidDAO.getAutoBidders(testRoomAutoId);
        assertTrue(updatedAutoBidders.stream().anyMatch(a ->
                TEST_AUTO_USER.equals(a.getUsername()) && a.getMaxPrice() == updatedMaxPrice),
                "Auto-bid list should show updated max price");

        // Step 7: Test removal
        boolean removed = AutoBidDAO.removeAutoBid(testRoomAutoId, TEST_AUTO_USER);
        assertTrue(removed, "Auto-bid should be removable");

        // Step 8: Verify removal
        long statusAfterRemoval = AutoBidDAO.checkAutoBidStatus(testRoomAutoId, TEST_AUTO_USER);
        assertEquals(-1, statusAfterRemoval, "Auto-bid should return -1 after removal");

        List<AutoBidSetting> afterRemovalList = AutoBidDAO.getAutoBidders(testRoomAutoId);
        assertFalse(afterRemovalList.stream().anyMatch(a -> TEST_AUTO_USER.equals(a.getUsername())),
                "Auto-bidder should not be in list after removal");
    }


    @Test
    public void testAuctionCompletionAndRoomStatusTransition() {
        String testProductId = "P_004";
        String testCompletionRoom = "completion_test_room";

        // Setup test product if not exists
        ProductDAO.addProducts(testProductId, "Completion Test Product", "TYPE", "Details", TEST_SELLER);

        Room newRoom = new Room(null, testCompletionRoom, testProductId, TEST_SELLER, 50000L,
                LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Room createdRoom = RoomDAO.createRoom(newRoom);
        assertNotNull(createdRoom, "Room should be created");
        String roomIdForCompletion = createdRoom.getRoomId();

        // Step 2: Verify initial status is PENDING
        Room initialRoom = RoomDAO.getRoomById(roomIdForCompletion);
        assertNotNull(initialRoom);
        assertEquals("PENDING", initialRoom.getStatus(), "New room should have PENDING status");

        // Step 3: Update room status to ACTIVE
        boolean activateSuccess = RoomDAO.updateRoomStatus(roomIdForCompletion, "ACTIVE");
        assertTrue(activateSuccess, "Room should be activated successfully");

        // Step 4: Verify status changed to ACTIVE
        Room activeRoom = RoomDAO.getRoomById(roomIdForCompletion);
        assertNotNull(activeRoom);
        assertEquals("ACTIVE", activeRoom.getStatus(), "Room should be ACTIVE");

        // Step 5: Place some bids in the active room
        long bidAmount = initialRoom.getStartingPrice() + Room.calculateDefaultBidStep(initialRoom.getStartingPrice());
        long bidderBalance = 1_000_000L - bidAmount;

        List<BidTransaction> existingBids = BidDAO.getBidHistory(activeRoom);
        boolean alreadyHasBid = existingBids.stream()
                .anyMatch(b -> TEST_BIDDER1.equals(b.getBidderUsername()) && b.getBidAmount() == bidAmount);

        if (!alreadyHasBid) {
            boolean bidPlaced = BidDAO.placeBid(activeRoom, null, TEST_BIDDER1, bidAmount, 0L, bidderBalance);
            assertTrue(bidPlaced, "Bid should be placed in active room");
        }

        // Step 6: Verify bid was recorded
        BidTransaction latestBid = BidDAO.getLatestBid(roomIdForCompletion);
        assertNotNull(latestBid);
        assertEquals(bidAmount, latestBid.getBidAmount());

        // Step 7: Close the auction (update status to CLOSED)
        boolean closeSuccess = RoomDAO.updateRoomStatus(roomIdForCompletion, "CLOSED");
        assertTrue(closeSuccess, "Room should be closed successfully");

        // Step 8: Verify room is now CLOSED
        Room closedRoom = RoomDAO.getRoomById(roomIdForCompletion);
        assertNotNull(closedRoom);
        assertEquals("CLOSED", closedRoom.getStatus(), "Room should be CLOSED");

        // Step 9: Verify bid history is preserved in closed room
        List<BidTransaction> closedRoomHistory = BidDAO.getBidHistory(closedRoom);
        assertNotNull(closedRoomHistory);
        assertTrue(closedRoomHistory.stream().anyMatch(b ->
                TEST_BIDDER1.equals(b.getBidderUsername()) && b.getBidAmount() == bidAmount),
                "Bid history should be preserved in closed room");

        // Step 10: Verify closed room is not in active rooms list
        List<Room> activeRooms = RoomDAO.getActiveRooms();
        assertFalse(activeRooms.stream().anyMatch(r -> roomIdForCompletion.equals(r.getRoomId())),
                "Closed room should not appear in active rooms list");
    }
}


