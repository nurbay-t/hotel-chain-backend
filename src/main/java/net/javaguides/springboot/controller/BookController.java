package net.javaguides.springboot.controller;

import net.javaguides.springboot.model.*;
import net.javaguides.springboot.payload.request.BookRequest;
import net.javaguides.springboot.repository.BookRepository;
import net.javaguides.springboot.repository.RoomRepository;
import net.javaguides.springboot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/book")
public class BookController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    public static int getDayNumberOld(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private double calculateBill(Date from, Date to, RoomType roomType) {
        double bill = 0;

        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            days.add(0);
        }

        while(from.getTime() <= to.getTime()) {
            int weekDay = getDayNumberOld(from);
            days.set(weekDay, days.get(weekDay) + 1);
            from.setTime(from.getTime() + 86400000);
        }

        bill += days.get(1) * roomType.getPrice_W();
        bill += days.get(2) * roomType.getPrice_R();
        bill += days.get(3) * roomType.getPrice_F();
        bill += days.get(4) * roomType.getPrice_St();
        bill += days.get(5) * roomType.getPrice_Sn();
        bill += days.get(6) * roomType.getPrice_M();
        bill += days.get(7) * roomType.getPrice_T();

        return bill;
    }

    @PostMapping("/room")
    public ResponseEntity<?> book(@Valid @RequestBody BookRequest bookRequest){

        Optional<User> optionalUser = userRepository.findById(bookRequest.getUserID());
        User user = optionalUser.get();
        Optional<Room> optionalRoom = roomRepository.findById(bookRequest.getRoomID());
        Room room = optionalRoom.get();

        Date fromDate = new Date(bookRequest.getFromDate().getTime());
        double bill = calculateBill(bookRequest.getFromDate(), bookRequest.getToDate(), room.getRoomType());

        Book book = new Book(fromDate, bookRequest.getToDate(), room, user, bill);
        bookRepository.save(book);

        return ResponseEntity.ok().body("You have booked!");
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateBook(@Valid @RequestBody BookRequest bookRequest){

        Optional<Book> optionalBook = bookRepository.findById(bookRequest.getBookID());
        Book book = optionalBook.get();
        Optional<Room> optionalRoom = roomRepository.findById(bookRequest.getRoomID());
        Room room = optionalRoom.get();

        Date fromDate = new Date(bookRequest.getFromDate().getTime());
        book.setFromDate(fromDate);
        book.setToDate(bookRequest.getToDate());
        book.setRoom(room);

        double bill = calculateBill(bookRequest.getFromDate(), bookRequest.getToDate(), room.getRoomType());
        book.setBill(bill);

        bookRepository.save(book);
        return ResponseEntity.ok().body("You have changed the booking!");
    }

    @GetMapping("/bill")
    public double getBill(@Valid @RequestBody BookRequest bookRequest){
        Optional<Book> optionalBook = bookRepository.findById(bookRequest.getBookID());
        Book book = optionalBook.get();
        Room room = book.getRoom();
        RoomType roomtype = room.getRoomType();
        Hotel hotel = roomtype.getHotel();
        List<Season> seasons = hotel.getSeasons();
        Date curDate = book.getFromDate();
        Season season = new Season();
        for(Season iSeason: seasons){
            if(curDate.after(iSeason.getStartDate()) && !curDate.after(iSeason.getEndDate())){
                season = iSeason;
                System.out.println("!");
            }
        }
        User user = book.getUser();
        UserCategory userCategory = user.getUserCategory();

        return book.getBill()*season.getCoefficient()*userCategory.getCoefficient();
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteBook(@Valid @RequestBody BookRequest bookRequest){
        Optional<Book> optionalBook = bookRepository.findById(bookRequest.getBookID());
        Book book = optionalBook.get();

        bookRepository.delete(book);
        return ResponseEntity.ok().body("You have deleted the booking!");
    }

    @GetMapping
    public List<Book> getAllBooks() { return bookRepository.findAll(); }
}
