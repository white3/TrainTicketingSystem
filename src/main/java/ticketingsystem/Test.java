package ticketingsystem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;

class TestResult {
	public double avgSingleBuyTicketTime;
	public double avgSingleRefundTime;
	public double avgSingleInquiryTime;
	public double throughput;

	public TestResult() {
	}

	public TestResult(double avgSingleBuyTicketTime, double avgRefundTime, double avgInquiryTime, double throughput) {
		this.avgSingleBuyTicketTime = avgSingleBuyTicketTime;//测试中每个线程执行购票操作耗时
		this.avgSingleRefundTime = avgRefundTime;//测试中每个线程执行退票操作耗时
		this.avgSingleInquiryTime = avgInquiryTime;//测试中每个线程执行查询操作耗时
		this.throughput = throughput;//系统总吞吐量
	}

	@Override
	public String toString() {
		BigDecimal b1 = new BigDecimal(avgSingleBuyTicketTime);
		avgSingleBuyTicketTime = b1.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();
		BigDecimal b2 = new BigDecimal(avgSingleInquiryTime);
		avgSingleInquiryTime = b2.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();
		BigDecimal b3 = new BigDecimal(avgSingleRefundTime);
		avgSingleRefundTime = b3.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();

		return "TestResult{" +
				"avgSingleBuyTicketTime=" + avgSingleBuyTicketTime + "ms" +
				", avgSingleRefundTime=" + avgSingleRefundTime + "ms" +
				", avgSingleInquiryTime=" + avgSingleInquiryTime + "ms" +
				", throughput=" + String.format("%.2f", throughput) + "times/s" +
				'}';
	}
}

public class Test {
        final static int routenum = 20; // route is designed from 1 to 20
        final static int coachnum = 10; // coach is arranged from 1 to 10
        final static int seatnum = 100; // seat is allocated from 1 to 20
        final static int stationnum = 16; // station is designed from 1 to 5

        final static int testnum = 200000;
        final static int retpc = 10; // return ticket operation is 10% percent
        final static int buypc = 20; // buy ticket operation is 30% percent
        final static int inqpc = 100; //inquiry ticket operation is 60% percent

	private final static int[] thread_nums = {64}; // concurrent thread number

	private final static int BUY = 1;
	private final static int REFUND = 0;
	private final static int INQUERY = 2;


	private String passengerName(int testnum) {
		Random rand = new Random();
		long uid = rand.nextInt(testnum);
		return "passenger" + uid;
	}

	private TestResult test(final int threadnum, final int testnum, final int routenum, final int coachnum, final int seatnum, final int stationnum) throws Exception {
		Thread[] threads = new Thread[threadnum];

		// 用于计算单线程方法平均耗时
		final long[] functionStartTime = new long[threadnum];
		final long[][] functionCostTimeSum = new long[threadnum][3];
		final int[][] executeCount = new int[threadnum][3];

		long startTime = System.currentTimeMillis();
		final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);

		for (int i = 0; i < threadnum; i++) {
			final int finalI = i;
			threads[i] = new Thread(new Runnable() {
				public void run() {
					Random rand = new Random();
					Ticket ticket;
					ArrayList<Ticket> soldTicket = new ArrayList<>();

					for (int j = 0; j < testnum; j++) {
						int sel = rand.nextInt(inqpc);
						if (0 <= sel && sel < retpc && soldTicket.size() > 0) { //  refund ticket
							int select = rand.nextInt(soldTicket.size());
							if ((ticket = soldTicket.remove(select)) != null) {
								functionStartTime[finalI] = System.currentTimeMillis();
								tds.refundTicket(ticket);
								executeCount[finalI][REFUND]++;
								functionCostTimeSum[finalI][REFUND] += System.currentTimeMillis() - functionStartTime[finalI];
							}
						} else if (retpc <= sel && sel < buypc) { // buy ticket
							String passenger = passengerName(testnum);
							int route = rand.nextInt(routenum) + 1;
							int departure = rand.nextInt(stationnum - 1) + 1;
							int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
							functionStartTime[finalI] = System.currentTimeMillis();
							if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {
								soldTicket.add(ticket);
							}
							executeCount[finalI][BUY]++;
							functionCostTimeSum[finalI][BUY] += System.currentTimeMillis() - functionStartTime[finalI];// though the action fails, the time counts
						} else if (buypc <= sel && sel < inqpc) { // inquiry ticket
							int route = rand.nextInt(routenum) + 1;
							int departure = rand.nextInt(stationnum - 1) + 1;
							int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
							functionStartTime[finalI] = System.currentTimeMillis();
							int leftTicket = tds.inquiry(route, departure, arrival);
							executeCount[finalI][INQUERY]++;
							functionCostTimeSum[finalI][INQUERY] += System.currentTimeMillis() - functionStartTime[finalI];
						}
					}
				}
			});
			threads[i].start();
		}

		for (int i = 0; i < threadnum; i++) {
			threads[i].join();
		}
		long totalTime = System.currentTimeMillis() - startTime; //获取总时间

		double avgBuy = 0, avgRefund = 0, avgInqui = 0, totalCount = 0;
		for (int i = 0; i < threadnum; i++) {
			totalCount += executeCount[i][BUY] + executeCount[i][REFUND] + executeCount[i][INQUERY];
			avgRefund += functionCostTimeSum[i][REFUND] * 1.0 / executeCount[i][REFUND];
			avgBuy += functionCostTimeSum[i][BUY] * 1.0 / executeCount[i][BUY];
			avgInqui += functionCostTimeSum[i][INQUERY] * 1.0 / executeCount[i][INQUERY];
		}
		avgBuy /= threadnum;
		avgRefund /= threadnum;
		avgInqui /= threadnum;

		return new TestResult(avgBuy, avgRefund, avgInqui, 1.0 * totalCount * 1000 / totalTime);
	}

	public static void main(String[] args) throws Exception {
		int each = 5;//多次测试取平均，此处为5次取平均

		Test test = new Test();
		TestResult[] testResult = new TestResult[thread_nums.length];
		for (int i = 0; i < thread_nums.length; i++) {
			testResult[i] = new TestResult();
			for (int j = 0; j < each; j++) {
				TestResult tmp = test.test(thread_nums[i],testnum,routenum,coachnum,seatnum,stationnum);
				testResult[i].avgSingleBuyTicketTime += tmp.avgSingleBuyTicketTime;
				testResult[i].avgSingleInquiryTime += tmp.avgSingleInquiryTime;
				testResult[i].avgSingleRefundTime += tmp.avgSingleRefundTime;
				testResult[i].throughput += tmp.throughput;
			}
			testResult[i].avgSingleBuyTicketTime /= each;
			testResult[i].avgSingleRefundTime /= each;
			testResult[i].avgSingleInquiryTime /= each;
			testResult[i].throughput /= each;
			System.out.println("Thread: " + thread_nums[i] + "\n" + testResult[i]);
		}
	}
}
