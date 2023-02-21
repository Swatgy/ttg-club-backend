package club.dnd5.portal.util;

import club.dnd5.portal.dto.api.RequestApi;
import club.dnd5.portal.dto.api.spells.Order;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

public class SortUtil {
	public static Sort getSort(RequestApi request) {
		if (request.getOrders() == null) {
			return Sort.unsorted();
		}
		return Sort.by(
			request.getOrders()
			.stream()
			.map(SortUtil::getOrder)
			.collect(Collectors.toList())
		);
	}

	private static Sort.Order getOrder(Order order) {
		if (order == null || order.getField() == null) {
			return Sort.Order.asc("name");
		}
		return order.getDirection().equalsIgnoreCase("asc") ? Sort.Order.asc(order.getField()) : Sort.Order.desc(order.getField());
	}
}
