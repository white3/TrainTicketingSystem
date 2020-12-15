package ticketingsystem;
import java.util.concurrent.atomic.AtomicLong;

public class TicketFactory {
    // To genarate the unique tid
	private static AtomicLong count = new AtomicLong();

	public static Ticket ConstructTicket(String passenger, int route, int departure, int arrival, int coach, int seat){
		Ticket t = new Ticket();
		t.tid = count.getAndIncrement();
		t.passenger = passenger;
		t.route = route;
		t.coach = coach;
		t.seat = seat;
		t.departure = departure;
		t.arrival = arrival;
		return t;
	}
}