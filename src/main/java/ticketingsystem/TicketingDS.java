package ticketingsystem;

public class TicketingDS implements TicketingSystem {
	private final int ROUTE_NUM; // 列车数量
	private final int COACH_NUM; // 车厢数量
	private final int SEAT_NUM; // 座位数量
	private SeatBitMap[] routeSeatBitMaps; // 所有列车的座位图

	/**
	 * 
	 * @param routenum   列车数量
	 * @param coachnum   车厢数量
	 * @param seatnum    座位数量
	 * @param stationnum 站点数量
	 * @param threadnum  线程数量
	 * @throws InterruptedException
	 */
	public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum)
			throws InterruptedException {
		// 验证数据是否合法
		if (stationnum > 64)
			throw new InterruptedException("不支持64个以上的站点");

		ROUTE_NUM = routenum;
		COACH_NUM = coachnum;
		SEAT_NUM = seatnum;

		routeSeatBitMaps = new SeatBitMap[ROUTE_NUM];
		for (int i = 0; i < ROUTE_NUM; i++) {
			// 初始化每节列车的座位
			routeSeatBitMaps[i] = new SeatBitMap(COACH_NUM, SEAT_NUM);
		}
	}

	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		int[] coach_seat;
		coach_seat = routeSeatBitMaps[route - 1].allocateSeat(departure, arrival);
		if (coach_seat[0] != 0)
			return TicketFactory.ConstructTicket(passenger, route, departure, arrival, coach_seat[0], coach_seat[1]);
		else
			return null;
	}

	@Override
	public int inquiry(int route, int departure, int arrival) {
		return routeSeatBitMaps[route - 1].query(departure, arrival);
	}

	@Override
	public boolean refundTicket(Ticket ticket) {
		try {
			return routeSeatBitMaps[ticket.route - 1].recycleSeat(ticket.departure, ticket.arrival, ticket.coach,
					ticket.seat);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
