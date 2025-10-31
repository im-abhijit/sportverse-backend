package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SlotsRepository slotsRepository;

    public List<BookingItemResponse> getUserBookings(String userId) {
        List<Document> bookingDocs = bookingRepository.findByUserId(userId);
        List<BookingItemResponse> responses = new ArrayList<>();

        for (Document doc : bookingDocs) {
            BookingItemResponse item = new BookingItemResponse();
            item.setId(doc.getObjectId("_id").toString());
            String venueId = doc.getObjectId("venueId").toString();
            String date = doc.getString("date");
            item.setDate(date);
            item.setAmount(doc.getInteger("amount", 0));
            item.setBookingStatus(doc.getString("bookingStatus"));

            Document paymentDoc = (Document) doc.get("payment");
            if (paymentDoc != null) {
                item.setPaymentStatus(paymentDoc.getString("status"));
            }

            Venue venue = venueRepository.findById(venueId);
            if (venue != null) {
                item.setVenue(new VenueResponse(venue));
            }

            @SuppressWarnings("unchecked")
            List<String> slotIds = (List<String>) doc.get("slotIds");
            if (slotIds != null && !slotIds.isEmpty()) {
                VenueSlots vs = slotsRepository.findByVenueIdAndDate(venueId, date);
                if (vs != null && vs.getSlots() != null) {
                    Set<String> target = new HashSet<>(slotIds);
                    List<TimeSlot> selected = new ArrayList<>();
                    for (TimeSlot s : vs.getSlots()) {
                        if (s.getSlotId() != null && target.contains(s.getSlotId())) {
                            selected.add(s);
                        }
                    }
                    item.setSlots(selected);
                } else {
                    item.setSlots(new ArrayList<>());
                }
            } else {
                item.setSlots(new ArrayList<>());
            }

            responses.add(item);
        }

        return responses;
    }
}


