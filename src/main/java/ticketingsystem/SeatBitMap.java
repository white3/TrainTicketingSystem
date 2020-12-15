package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SeatBitMap {
	// private ConcurrentHashMap<Long, AtomicInteger> cache 
	// 	= new ConcurrentHashMap<Long, AtomicInteger>(); // 检索记录缓存, <座位号,下一个空位的索引号>
	private AtomicLong[] seatBitMap; // 标记整个列车的座位, 使用long存储该座位 出发-终点站.
	private final int COACH_NUM; // 车厢数
	private final int SEAT_NUM; // 每节车厢座位数
	private final int TOTAL_SEAT_NUM; // 座位总数

	public SeatBitMap(int coachMum, int seatNum) {
		COACH_NUM = coachMum;
		SEAT_NUM = seatNum;
		TOTAL_SEAT_NUM = COACH_NUM * SEAT_NUM;

		seatBitMap = new AtomicLong[TOTAL_SEAT_NUM];
		for (int i = 0; i < TOTAL_SEAT_NUM; i++) {
			seatBitMap[i] = new AtomicLong();
		}
	}

	/**
	 * 使用bit来表示某对应区段的座位是否被占用, 1表示占用
	 *
	 * @param departure 始发站
	 * @param arrival   终点站
	 * @return {车厢号， 座位号}, 如果返回[0, 0], 表示没有位置
	 * @throws Exception
	 */
	public int[] allocateSeat(int departure, int arrival) {
		int[] result = { 0, 0 };

		// 构造检索者(检索bits)
		long searcher = getSearcher(departure, arrival);

		// 如果不存在，构造新的记录
		// if (!cache.containsKey(searcher)) {
		// 	cache.put(searcher, new AtomicInteger());
		// }

		// 新座位bit标记, 原座位bit标记
		long newSeatFlagBit, seatFlagBit;
		// AtomicInteger currCache = cache.get(searcher);
		// int currIndex = currCache.get();

		// 遍历寻找座位
		for (int i = 0; i < TOTAL_SEAT_NUM; i++) {
		// for (int i = currIndex; i < TOTAL_SEAT_NUM; i++) {
			AtomicLong currFlagBit = seatBitMap[i];
			seatFlagBit = currFlagBit.get();
			// 找到检索者范围内bit值都为0的座位, 即座位为空
			// TODO while换为if ?
			while ((seatFlagBit & searcher) == 0) {
				// 将对应区段都改为1标志
				newSeatFlagBit = seatFlagBit | searcher;
				if (currFlagBit.compareAndSet(seatFlagBit, newSeatFlagBit)) {
					result = new int[] { i / SEAT_NUM + 1, i % SEAT_NUM + 1 };
					// 更新最近购票位
					// currCache.compareAndSet(currIndex, i + 1);
					return result;
				}
				seatFlagBit = currFlagBit.get();
			}
		}
		return result;
	}

	/**
	 * 回收退票对应的座位
	 *
	 * @param departure 始发站
	 * @param arrival   终点站
	 * @param coach     车厢号
	 * @param seat      座位号
	 * @return
	 * @throws Exception
	 */
	public boolean recycleSeat(int departure, int arrival, int coach, int seat) throws Exception {
		int index = (coach - 1) * SEAT_NUM + seat - 1;
		// System.out.printf("%d = (%d - 1) * %d + %d - 1", index, coach, SEAT_NUM,
		// seat);
		AtomicLong currFlagBit = seatBitMap[index];
		long seatFlagBit = currFlagBit.get();
		long newSeatFlagBit;

		// 获取余票搜索者
		long searcher = getSearcher(departure, arrival);

		// 在探测器范围内的bit都应该是1，否则出错
		// AtomicInteger currCache = cache.get(searcher);
		while ((seatFlagBit & searcher) == searcher) {
			newSeatFlagBit = (~searcher) & seatFlagBit;
			if (currFlagBit.compareAndSet(seatFlagBit, newSeatFlagBit)) {
				// TODO 如何保证小于历史的index一定都被记录到？
				// int expect = currCache.get();
				// while (index < expect) { // allocateSeat时, 将产生一个新值
				// 	// System.out.printf("index = %d, expect = %d", index, expect);
				// 	currCache.compareAndSet(expect, index);
				// 	expect = currCache.get();
				// }
				return true;
			}
			seatFlagBit = currFlagBit.get();
		}
		System.out.println("Can't recycle seat, something wrong!");
		System.out.flush();
		return false;
	}

	/**
	 * 返回剩余的车票数
	 *
	 * @param departure 始发站
	 * @param arrival   终点站
	 * @return 余票
	 */
	public int query(int departure, int arrival) {
		int count = 0;
		long searcher = getSearcher(departure, arrival);

		// int currIndex = 0;
		// 查找记录, 若无记录检索所有
		// if (cache.containsKey(searcher)) {
		// 	AtomicInteger currCache = cache.get(searcher);
		// 	currIndex = currCache.get();
		// }
		for (int i = 0; i < TOTAL_SEAT_NUM; i++) {
			// for (int i = currIndex; i < TOTAL_SEAT_NUM; i++) {
			AtomicLong currSeatBit = seatBitMap[i];
			if ((currSeatBit.get() & searcher) == 0)
				count++;
		}
		return count;
	}

	/**
	 * 返回对应区间的余票检测器
	 *
	 * @param departure
	 * @param arrival
	 * @return
	 */
	private long getSearcher(int departure, int arrival) {
		long searcher = 0;
		for (int i = departure - 1; i < arrival - 1; i++) // departure - 1 是由于数组0开始, arrival - 1是由于在arrival处为下车站, 不需要座位
			searcher += 1 << i;
		return searcher;
	}
}
