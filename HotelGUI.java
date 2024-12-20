import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Room {
    int roomNumber;
    boolean isBooked;
    String guestName;

    public Room(int roomNumber) {
        this.roomNumber = roomNumber;
        this.isBooked = false;
    }

    public void bookRoom(String guestName) {
        if (!isBooked) {
            this.guestName = guestName;
            this.isBooked = true;
            System.out.println("Room " + roomNumber + " booked successfully for " + guestName);
        } else {
            System.out.println("Room " + roomNumber + " is already booked.");
        }
    }

    public void releaseRoom() {
        if (isBooked) {
            System.out.println("Room " + roomNumber + " released from " + guestName);
            this.isBooked = false;
            this.guestName = null;
        } else {
            System.out.println("Room " + roomNumber + " is not currently booked.");
        }
    }
}

class Hotel {
    ArrayList<Room> rooms;

    public Hotel(int numRooms) {
        rooms = new ArrayList<>(numRooms);
        for (int i = 1; i <= numRooms; i++) {
            rooms.add(new Room(i));
        }
    }

    public void displayStatus() {
        for (Room room : rooms) {
            System.out.println("Room " + room.roomNumber + " is " + (room.isBooked ? "booked by " + room.guestName : "available"));
        }
    }

    public void bookRoom(int roomNumber, String guestName) {
        if (roomNumber > 0 && roomNumber <= rooms.size()) {
            rooms.get(roomNumber - 1).bookRoom(guestName);
        } else {
            System.out.println("Invalid room number.");
        }
    }

    public void releaseRoom(int roomNumber) {
        if (roomNumber > 0 && roomNumber <= rooms.size()) {
            rooms.get(roomNumber - 1).releaseRoom();
        } else {
            System.out.println("Invalid room number.");
        }
    }

    public void loadRoomsFromDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms");
            while (rs.next()) {
                int roomNumber = rs.getInt("room_number");
                boolean isBooked = rs.getBoolean("is_booked");
                String guestName = rs.getString("guest_name");
                Room room = new Room(roomNumber);
                room.isBooked = isBooked;
                room.guestName = guestName;
                rooms.add(room);
            }
            System.out.println("Rooms loaded from the database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateRoomInDatabase(Room room, Connection conn) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE rooms SET is_booked = ?, guest_name = ? WHERE room_number = ?")) {
            pstmt.setBoolean(1, room.isBooked);
            pstmt.setString(2, room.guestName);
            pstmt.setInt(3, room.roomNumber);
            pstmt.executeUpdate();
            System.out.println("Room " + room.roomNumber + " updated in the database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

public class HotelGUI extends JFrame {
    private Hotel hotel;
    private JTextField roomNumberField, guestNameField;
    private JTextArea displayArea;
    private Connection conn;

    public HotelGUI(int numRooms) {
        hotel = new Hotel(numRooms);
        initComponents();
        createConnection();
        hotel.loadRoomsFromDatabase(conn);
    }

    private void createConnection() {
        String url = "jdbc:mysql://sql12.freesqldatabase.com:3306/sql12753166";
        String user = "sql12753166";
        String password = "qu1qMvT6BT";
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        setTitle("Hotel Management System");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Room Number:"));
        roomNumberField = new JTextField();
        panel.add(roomNumberField);
        panel.add(new JLabel("Guest Name:"));
        guestNameField = new JTextField();
        panel.add(guestNameField);

        JButton displayButton = new JButton("Display Room Status");
        JButton bookButton = new JButton("Book Room");
        JButton releaseButton = new JButton("Release Room");

        panel.add(displayButton);
        panel.add(bookButton);
        panel.add(releaseButton);

        add(panel, BorderLayout.NORTH);

        displayArea = new JTextArea();
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        displayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayStatus();
            }
        });

        bookButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bookRoom();
            }
        });

        releaseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                releaseRoom();
            }
        });

        setVisible(true);
    }

    private void displayStatus() {
        displayArea.setText("");
        for (Room room : hotel.rooms) {
            displayArea.append("Room " + room.roomNumber + " is " + (room.isBooked ? "booked by " + room.guestName : "available") + "\n");
        }
    }

    private void bookRoom() {
        try {
            int roomNumber = Integer.parseInt(roomNumberField.getText());
            String guestName = guestNameField.getText();
            hotel.bookRoom(roomNumber, guestName);
            Room bookedRoom = hotel.rooms.get(roomNumber - 1);
            hotel.updateRoomInDatabase(bookedRoom, conn);
            displayArea.setText("Room " + roomNumber + " booked successfully for " + guestName + "\n");
        } catch (NumberFormatException e) {
            displayArea.setText("Please enter a valid room number.\n");
        }
    }

    private void releaseRoom() {
        try {
            int roomNumber = Integer.parseInt(roomNumberField.getText());
            hotel.releaseRoom(roomNumber);
            Room releasedRoom = hotel.rooms.get(roomNumber - 1);
            hotel.updateRoomInDatabase(releasedRoom, conn);
            displayArea.setText("Room " + roomNumber + " released.\n");
        } catch (NumberFormatException e) {
            displayArea.setText("Please enter a valid room number.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotelGUI(10)); // Assuming 10 rooms for this example
    }
}