package client.controllers;

import common.Request;
import common.models.TransactionRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import server.dao.UserDAO;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class WalletController {
    @FXML private ComboBox<String> cbDepositMethod;
    @FXML private ComboBox<String> cbWithdrawBank;
    @FXML private Label status;
    @FXML private Label lblBalance;
    @FXML private TextField txtDepositAmount;
    @FXML private TextField txtWithdrawAmount;
    @FXML private Button btnConfirmDeposit;
    @FXML private Button btnConfirmWithdraw;

    @FXML private Label lblDepositError;
    @FXML private Label lblWithdrawError;

    @FXML private TableView<TransactionRecord> tblTransactionHistory;
    @FXML private TableColumn<TransactionRecord, String> colDate;
    @FXML private TableColumn<TransactionRecord, String> colType;
    @FXML private TableColumn<TransactionRecord, String> colAmount;
    @FXML private TableColumn<TransactionRecord, String> colStatus;

    private ObservableList<TransactionRecord> historyList = FXCollections.observableArrayList();

    private long currentBalance = 0;
    private final String NORMAL_STYLE = "-fx-border-color: #CED4DA; -fx-border-radius: 5;";

    @FXML
    public void initialize() {
        setupTextField(txtDepositAmount, lblDepositError);
        setupTextField(txtWithdrawAmount, lblWithdrawError);

        cbDepositMethod.setItems(FXCollections.observableArrayList(
                "Wallet"
        ));

        cbWithdrawBank.setItems(FXCollections.observableArrayList(
                "Bank"
        ));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblTransactionHistory.setItems(historyList);

        // số dư từ Server ngay khi mở UI
        loadWalletInfo();


        btnConfirmDeposit.setOnAction(this::handleDeposit);
        btnConfirmWithdraw.setOnAction(this::handleWithdraw);
    }

    private void loadWalletInfo() {
        lblBalance.setText("Đang tải...");

        // Sử dụng username lấy từ Session
        String username = Session.getInstance().getCurrentUsername();
        Request req = new Request("GET_WALLET_INFO", username);

        Session.getInstance().sendRequest(req, response -> {
            Platform.runLater(() -> {
                if(response.getAction().equals("GET_WALLET_SUCCESS")) {
                    this.currentBalance = Long.parseLong((String) response.getData());
                    lblBalance.setText(formatCurrency(this.currentBalance));
                    Session.getInstance().getCurrentUser().setBalance(this.currentBalance);

                    loadTransactionHistory();
                } else {
                    lblBalance.setText("Lỗi tải dữ liệu");
                }
            });
        });
    }
    private void setupTextField(TextField inputField, Label errorLabel) {
        inputField.setStyle(NORMAL_STYLE);
        inputField.textProperty().addListener((observable, oldValue, newValue) -> {
            inputField.setStyle(NORMAL_STYLE);
            errorLabel.setText("");
            if (!newValue.matches("\\d*")) {
                inputField.setText(newValue.replaceAll("\\D", ""));
            }
        });
    }

    private void showError(TextField inputField, Label errorLabel, String message) {
        String ERROR_STYLE = "-fx-border-color: red; -fx-border-radius: 5;";
        inputField.setStyle(ERROR_STYLE);
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
    }

    private void showSuccess(TextField inputField, Label errorLabel, String message) {
        inputField.setStyle(NORMAL_STYLE);
        inputField.clear();
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void handleDeposit(ActionEvent event) {
        String input = txtDepositAmount.getText().trim();
        String method = cbDepositMethod.getValue();

        if (input.isEmpty()) {
            showError(txtDepositAmount, lblDepositError, "* Vui lòng nhập số tiền cần nạp!");
            return;
        }
        if (method == null) {
            showError(txtDepositAmount, lblDepositError, "* Vui lòng chọn phương thức nạp!");
            return;
        }

        long amount = Long.parseLong(input);
        if (amount < 10000) {
            showError(txtDepositAmount, lblDepositError, "* Số tiền nạp tối thiểu là 10,000 VNĐ!");
            return;
        }


        String username = Session.getInstance().getCurrentUsername();
        String[] info = { username, String.valueOf(amount), method };

        Request depositReq = new Request("DEPOSIT", info);
        Session.getInstance().sendRequest(depositReq, response -> {
            Platform.runLater(() -> {
                if(response.getAction().equals("DEPOSIT_SUCCESS")){
                    showSuccess(txtDepositAmount, lblDepositError, "Đã nạp " + formatCurrency(amount) + " thành công!");
                    loadWalletInfo();
                } else {
                    showError(txtDepositAmount, lblDepositError, "Thất bại: " + response.getData());
                }
            });
        });
    }

    private void handleWithdraw(ActionEvent event) {
        String input = txtWithdrawAmount.getText().trim();
        String bank = cbWithdrawBank.getValue();

        if (input.isEmpty()) {
            showError(txtWithdrawAmount, lblWithdrawError, "* Vui lòng nhập số tiền cần rút!");
            return;
        }
        if (bank == null) {
            showError(txtWithdrawAmount, lblWithdrawError, "* Vui lòng chọn ngân hàng!");
            return;
        }

        long amount = Long.parseLong(input);
        if (amount <= 0) {
            showError(txtWithdrawAmount, lblWithdrawError, "* Số tiền rút phải lớn hơn 0!");
            return;
        }
        if (amount > currentBalance) {
            showError(txtWithdrawAmount, lblWithdrawError, "* Số dư không đủ để thực hiện giao dịch này!");
            return;
        }

        String username = Session.getInstance().getCurrentUsername();
        String[] info = { username, String.valueOf(amount), bank};

        Request withdrawReq = new Request("WITHDRAW", info);
        Session.getInstance().sendRequest(withdrawReq, response -> {
            Platform.runLater(() -> {
                if(response.getAction().equals("WITHDRAW_SUCCESS")){
                    showSuccess(txtWithdrawAmount, lblWithdrawError, "✔ Đã rút " + formatCurrency(amount) + " thành công!");

                    loadWalletInfo();
                } else {
                    showError(txtWithdrawAmount, lblWithdrawError, "Thất bại: " + response.getData());
                }
            });
        });
    }

    private void loadTransactionHistory() {
        Request req = new Request("GET_HISTORY", null);

        Session.getInstance().sendRequest(req, response -> {
            Platform.runLater(() -> {
                if (response.getAction().equals("GET_HISTORY_SUCCESS")) {
                    List<TransactionRecord> data = (List<TransactionRecord>) response.getData();
                    historyList.clear();
                    historyList.addAll(data);
                }
            });
        });
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VNĐ";
    }

    @FXML
    public void goBack(ActionEvent event){
        String role = UserDAO.getUserRole(Session.getInstance().getCurrentUsername());
        if (role != null && role.equals("SELLER")) {
            SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
        }else if (role != null && role.equals("BIDDER")) {
            SceneController.switchScene("/client/views/bidder/BidderDashboard.fxml");
        }

    }
}