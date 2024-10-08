package controller;

import model.*;

import java.sql.*;
import java.util.ArrayList;

public class DatabaseConnected {
    private static final String URL = "jdbc:mysql://localhost:3306/apartment";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Kết nối đến cơ sở dữ liệu
    public static Connection getConnection() throws DatabaseConnectionException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            if (connection != null && !connection.isClosed()) {
                return connection;
            } else {
                throw new DatabaseConnectionException("Không thể kết nối đến cơ sở dữ liệu.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Lỗi khi kết nối đến cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }

    // Đóng kết nối cơ sở dữ liệu
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
            }
        }
    }

    // Lớp ngoại lệ tùy chỉnh cho kết nối cơ sở dữ liệu
    public static class DatabaseConnectionException extends Exception {
        public DatabaseConnectionException(String message) {
            super(message);
        }

        public DatabaseConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    /*  RESIDENT  */

    // Lấy dữ liệu cư dân và trả về dưới dạng ArrayList<Object[]>
    public static ArrayList<Object[]> getResidentsData() {
        String query = "SELECT id, full_name, date_of_birth, gender, id_card, is_temp_resident, household_id FROM residents";
        Connection connection = null;
        ArrayList<Object[]> residentsData = new ArrayList<>(); // Sử dụng ArrayList

        try {
            connection = getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = statement.executeQuery(query);

            // Duyệt qua các hàng trong ResultSet và thêm vào ArrayList
            int index = 0;
            while (resultSet.next()) {
                Object[] row = new Object[8];
                row[0] = String.format("%02d", index + 1);
                row[1] = resultSet.getString("full_name");
                row[2] = resultSet.getString("date_of_birth");
                row[3] = resultSet.getString("gender");
                row[4] = resultSet.getString("id_card");
                row[5] = resultSet.getString("is_temp_resident");
                if (resultSet.getString("household_id") != null) {
                    row[6] = resultSet.getString("household_id");
                } else {
                    row[6] = "";
                }
                row[7] = resultSet.getString("id");
                residentsData.add(row);
                index++;
            }

            resultSet.close();
            statement.close();
        } catch (SQLException | DatabaseConnectionException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }

        return residentsData;
    }

    // Thêm nhân khẩu
    public static void addResident(String name, String birthDate, String gender, String idCard) {
        String query = "INSERT INTO residents (full_name, date_of_birth, gender, id_card) VALUES (?, ?, ?, ?)";
        Connection connection = null;
        try {
            connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setDate(2, java.sql.Date.valueOf(birthDate));
            preparedStatement.setString(3, gender);
            preparedStatement.setString(4, idCard);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException | DatabaseConnectionException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }

    // Phương thức mới để cập nhật cư dân
    public static boolean updateResident(int residentID, String name, String birthDate, String gender, String idCard) {
        String sql = "UPDATE residents SET full_name = ?, date_of_birth = ?, gender = ?, id_card = ? WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            // Thiết lập các tham số cho PreparedStatement
            pstmt.setString(1, name);
            pstmt.setString(2, birthDate);
            pstmt.setString(3, gender);
            pstmt.setString(4, idCard);
            pstmt.setInt(5, residentID);

            // Thực thi câu lệnh cập nhật
            int affectedRows = pstmt.executeUpdate();

            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Xoá nhân khẩu
    public static boolean deleteResident(int residentID) {
        String sql = "DELETE FROM residents WHERE id = ?";

        try (Connection connection = DatabaseConnected.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Set the residentID in the query
            preparedStatement.setInt(1, residentID);

            // Execute the update
            int rowsAffected = preparedStatement.executeUpdate();

            // Return true if delete was successful
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Lấy dữ liệu hộ khẩu và trả về dưới dạng ArrayList<Object[]>
    public static ArrayList<Object[]> getHouseholdsData() {
        ArrayList<Object[]> householdsData = new ArrayList<>();
        String query = "SELECT h.id, h.address, r.id, r.full_name, r.date_of_birth, r.gender, r.id_card " +
                "FROM households h " +
                "JOIN residents r ON h.head_of_household = r.id";

        try (Connection conn = DatabaseConnected.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Duyệt qua các hàng trong ResultSet và thêm vào ArrayList
            int index = 0;
            while (rs.next()) {
                Object[] row = new Object[4];
                row[0] = String.format("%02d", index + 1);
                row[1] = rs.getInt("h.id");
                row[2] = rs.getString("h.address");

                // Tạo đối tượng Resident (chủ hộ) từ kết quả truy vấn
                Resident headOfHousehold = new Resident(rs.getInt("r.id"),
                        rs.getString("r.full_name"),
                        rs.getString("r.date_of_birth"),
                        rs.getString("r.gender"),
                        rs.getString("r.id_card"));
                row[3] = headOfHousehold;

                householdsData.add(row);
                index++;
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }

        return householdsData;
    }


    /*  HOUSEHOLD  */

    // Phương thức tìm thông tin của chủ hộ dựa trên họ tên và CCCD
    public static Resident getHeadOfHouseholdInfo(String fullName, String idCard) throws SQLException {
        String sql = "SELECT id, full_name, date_of_birth, gender, id_card FROM residents WHERE full_name = ? AND id_card = ?";
        try (Connection conn = DatabaseConnected.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Thiết lập các tham số cho PreparedStatement
            pstmt.setString(1, fullName);
            pstmt.setString(2, idCard);

            // Thực thi truy vấn
            ResultSet rs = pstmt.executeQuery();

            // Kiểm tra kết quả và trả về đối tượng Resident nếu tìm thấy
            if (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("full_name");
                String birthDate = rs.getString("date_of_birth");
                String gender = rs.getString("gender");
                String idCardNumber = rs.getString("id_card");

                // Tạo và trả về đối tượng Resident
                return new Resident(id, name, birthDate, gender, idCardNumber);
            } else {
                return null; // Trả về null nếu không tìm thấy cư dân
            }
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Phương thức thêm hộ khẩu mới vào cơ sở dữ liệu và trả về household_id vừa được tạo
    public static int addHousehold(String address, int headOfHouseholdId) throws SQLException {
        String sql = "INSERT INTO households (address, head_of_household) VALUES (?, ?)";
        try (Connection conn = DatabaseConnected.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Thiết lập các tham số cho PreparedStatement
            pstmt.setString(1, address);
            pstmt.setInt(2, headOfHouseholdId);

            // Thực thi câu lệnh thêm mới
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Lấy household_id vừa được tạo từ bảng households
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);  // Trả về household_id vừa được thêm
                }
            }
            return -1; // Trả về -1 nếu không thêm được hộ khẩu
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Phương thức cập nhật hộ khẩu trong cơ sở dữ liệu
    public static boolean updateHousehold(int householdID, String newAddress, int newHeadOfHouseholdID) throws SQLException {
        String sql = "UPDATE households SET address = ?, head_of_household = ? WHERE id = ?";
        try (Connection conn = DatabaseConnected.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Thiết lập các tham số cho PreparedStatement
            pstmt.setString(1, newAddress);
            pstmt.setInt(2, newHeadOfHouseholdID);
            pstmt.setInt(3, householdID);

            // Thực thi câu lệnh cập nhật
            int affectedRows = pstmt.executeUpdate();

            // Trả về true nếu cập nhật thành công
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Phương thức xóa hộ khẩu trong cơ sở dữ liệu
    public static boolean deleteHousehold(int householdID) {
        String sql = "DELETE FROM households WHERE id = ?";
        try (Connection conn = DatabaseConnected.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, householdID);

            // Thực thi câu lệnh xóa
            int affectedRows = pstmt.executeUpdate();

            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Phương thức lấy thông tin cư dân theo ID
    public static Resident getResidentById(int residentId) {
        String query = "SELECT * FROM residents WHERE id = ?";
        Resident resident = null;

        try (Connection conn = DatabaseConnected.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, residentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                resident = new Resident(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("date_of_birth"),
                        rs.getString("gender"),
                        rs.getString("id_card")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
        return resident;
    }

    // Phương thức lấy thành viên vs quan hệ
    public static ArrayList<HouseholdMember> getHouseholdMembersWithRelationships(int householdID) {
        ArrayList<HouseholdMember> members = new ArrayList<>();
        String query = "SELECT r.*, h.relationship_type FROM relationships h  JOIN residents r ON h.resident_id = r.id  WHERE h.household_id = ?";

        try (Connection conn = DatabaseConnected.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, householdID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Lấy thông tin resident từ kết quả truy vấn
                Resident resident = new Resident(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("date_of_birth"),
                        rs.getString("gender"),
                        rs.getString("id_card")
                );

                // Lấy relationship_type từ kết quả truy vấn
                String relationshipType = rs.getString("relationship_type");

                // Thêm đối tượng HouseholdMember vào danh sách
                members.add(new HouseholdMember(resident, relationshipType));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }

        return members;
    }

    // Thêm quan hệ
    public static boolean addRelationship(int residentID, int headOfHouseholdID, String relationshipType, int householdID) {
        String query = "INSERT INTO relationships (resident_id, head_of_household_id, relationship_type, household_id) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnected.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            // Set the query parameters
            stmt.setInt(1, residentID);
            stmt.setInt(2, headOfHouseholdID);
            stmt.setString(3, relationshipType);
            stmt.setInt(4, householdID);

            // Execute the update
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    /*  FEE  */
    public static ArrayList<Object[]> getFeesData() {
        ArrayList<Object[]> feesData = new ArrayList<>();
        String query = "SELECT id, fee_name, amount, fee_description, created_at, updated_at, status FROM fees"; // Giả định tên bảng là 'fees'

        try (Connection conn = DatabaseConnected.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            int index = 0;
            while (rs.next()) {
                Object[] row = new Object[8];
                row[0] = String.format("%02d", index + 1);
                row[1] = rs.getInt("id");
                row[2] = rs.getString("fee_name");
                row[3] = rs.getDouble("amount");
                row[4] = rs.getString("fee_description");
                row[5] = rs.getTimestamp("created_at");
                row[6] = rs.getTimestamp("updated_at");
                row[7] = rs.getString("status");

                feesData.add(row);
                index++;
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }

        return feesData;
    }

    public static int addFee(String feeName, String feeDescription, double amount) {
        String query = "INSERT INTO fees (fee_name, fee_description, amount) VALUES (?, ?, ?)";
        int generatedId = -1;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, feeName);
            pstmt.setString(2, feeDescription);
            pstmt.setDouble(3, amount);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedId = generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }

        return generatedId;
    }

    public static boolean editFee(int feeId, String feeName, String feeDescription, double amount) {
        String query = "UPDATE fees SET fee_name = ?, fee_description = ?, amount = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, feeName);
            pstmt.setString(2, feeDescription);
            pstmt.setDouble(3, amount);
            pstmt.setInt(4, feeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteFee(int feeID) {
        String query = "DELETE FROM fees WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, feeID);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    /*  PAYMENT  */
    //Lấy data cho dropdown
    public static ArrayList<Object[]> getFeesDropdown() {
        ArrayList<Object[]> fees = new ArrayList<>();
        String query = "SELECT id, fee_name FROM fees";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int fee_id = resultSet.getInt("id");
                String fee_name = resultSet.getString("fee_name");
                fees.add(new Object[]{fee_id, fee_name});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        }

        return fees;
    }

    // Phương thức lấy thông tin từ bảng households theo household_id
    private static Household getHouseholdById(int householdId, Connection conn) throws SQLException {
        String sql = "SELECT `id`, `address`, `head_of_household` FROM `households` WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, householdId);
        ResultSet rs = pstmt.executeQuery();

        Household household = null;
        if (rs.next()) {
            int headOfHouseholdId = rs.getInt("head_of_household");

            // Lấy thông tin của head_of_household từ bảng residents
            Resident headOfHousehold = getResidentById(headOfHouseholdId);

            // Tạo đối tượng Household bao gồm cả đối tượng Resident cho head_of_household
            household = new Household(rs.getInt("id"), rs.getString("address"), headOfHousehold);
        }
        rs.close();
        pstmt.close();
        return household;
    }

    // Phương thức lấy thông tin từ bảng fees theo fee_id
    private static Fee getFeeById(int feeId, Connection conn) throws SQLException {
        String sql = "SELECT `id`, `fee_name`, `fee_description`, `amount`, `created_at`, `updated_at`, `status` FROM `fees` WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, feeId);
        ResultSet rs = pstmt.executeQuery();

        Fee fee = null;
        if (rs.next()) {
            fee = new Fee(rs.getInt("id"), rs.getString("fee_name"), rs.getString("fee_description"),
                    rs.getDouble("amount"), rs.getTimestamp("created_at"),
                    rs.getTimestamp("updated_at"), rs.getString("status"));
        }
        rs.close();
        pstmt.close();
        return fee;
    }

    // Method to get all payments for a specific household_id and fee_id
    private static ArrayList<Payment> getPaymentsByHouseholdAndFee(int householdId, int feeId, Connection conn) throws SQLException {
        ArrayList<Payment> paymentRecords = new ArrayList<>();

        String sql = "SELECT `id`, `payment_amount`, `payment_date`, `payment_method`, `note` FROM `payments` WHERE household_id = ? AND fee_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, householdId);
            pstmt.setInt(2, feeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Tạo một đối tượng Payment mới
                    Payment payment = new Payment(
                            rs.getInt("id"),
                            rs.getInt("payment_amount"),
                            rs.getTimestamp("payment_date"),
                            rs.getString("payment_method"),
                            rs.getString("note")
                    );
                    paymentRecords.add(payment);
                }
            }
        }
        return paymentRecords;
    }


    // Phương thức mới lấy thông tin từ bảng payments dựa trên household_id và fee_id
    private static Payment getPaymentByHouseholdAndFeeId(int householdId, int feeId, Connection conn) throws SQLException {
        PreparedStatement paymentStmt = null;
        ResultSet paymentRs = null;
        Payment payment = null;

        try {
            String paymentSql = "SELECT `id`, `payment_amount`, `payment_date`, `payment_method`, `note` FROM `payments` WHERE household_id = ? AND fee_id = ?";
            paymentStmt = conn.prepareStatement(paymentSql);
            paymentStmt.setInt(1, householdId);
            paymentStmt.setInt(2, feeId);
            paymentRs = paymentStmt.executeQuery();

            if (paymentRs.next()) {
                payment = new Payment(
                        paymentRs.getInt("id"),
                        paymentRs.getInt("payment_amount"),
                        paymentRs.getTimestamp("payment_date"),
                        paymentRs.getString("payment_method"),
                        paymentRs.getString("note")
                );
            }

        } finally {
            if (paymentRs != null) try { paymentRs.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (paymentStmt != null) try { paymentStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        return payment;
    }

    // Lấy data cho payment của khoản phí có fee_id
    public static ArrayList<Object[]> getPaymentData(int fee_Id) {
        ArrayList<Object[]> paymentData = new ArrayList<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int index = 1;

        try {
            // Kết nối cơ sở dữ liệu
            conn = DatabaseConnected.getConnection();

            // Bước 1: Lấy dữ liệu từ bảng households_fees theo fee_id
            String sql = "SELECT `id`, `household_id`, `fee_id`, `amount_due`, `due_date`, `status`, `created_at`, `updated_at` FROM `households_fees` WHERE fee_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, fee_Id);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Object[] paymentRecord = new Object[10];

                // 0: Index
                paymentRecord[0] = index++;

                // 1: id (households_fees)
                int householdFeeId = rs.getInt("id");
                paymentRecord[1] = householdFeeId;

                // 2: Lấy household_id -> Lấy thông tin từ bảng households
                int householdId = rs.getInt("household_id");
                Household household = getHouseholdById(householdId, conn);
                paymentRecord[2] = household;

                // 3: Lấy fee_id -> Lấy thông tin từ bảng fees
                int feeId = rs.getInt("fee_id");
                Fee fee = getFeeById(feeId, conn);
                paymentRecord[3] = fee;

                // 4: amount_due
                paymentRecord[4] = rs.getDouble("amount_due");

                // 5: due_date
                paymentRecord[5] = rs.getDate("due_date");

                // 6: status
                paymentRecord[6] = rs.getString("status");

                // 7: created_at
                paymentRecord[7] = rs.getTimestamp("created_at");

                // 8: updated_at
                paymentRecord[8] = rs.getTimestamp("updated_at");

                // 9: Lấy thông tin thanh toán từ bảng payments qua phương thức mới
                ArrayList<Payment> payments = getPaymentsByHouseholdAndFee(householdId, feeId, conn);
                paymentRecord[9] = payments;

                // Thêm record vào danh sách
                paymentData.add(paymentRecord);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException(e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        return paymentData;
    }


}
