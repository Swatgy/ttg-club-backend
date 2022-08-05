package club.dnd5.portal.controller.api.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.Column;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.Search;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import club.dnd5.portal.dto.api.FilterApi;
import club.dnd5.portal.dto.api.FilterValueApi;
import club.dnd5.portal.dto.api.item.ItemApi;
import club.dnd5.portal.dto.api.item.ItemRequesApi;
import club.dnd5.portal.model.book.Book;
import club.dnd5.portal.model.book.TypeBook;
import club.dnd5.portal.model.items.Treasure;
import club.dnd5.portal.model.items.TreasureType;
import club.dnd5.portal.model.splells.Spell;
import club.dnd5.portal.repository.datatable.TreasureDatatableRepository;
import club.dnd5.portal.util.SpecificationUtil;

@RestController
public class TreasureApiController {
	@Autowired
	private TreasureDatatableRepository treasuryRepository;

	@PostMapping(value = "/api/v1/treasures", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ItemApi> getItem(@RequestBody ItemRequesApi request) {
		Specification<Treasure> specification = null;

		DataTablesInput input = new DataTablesInput();
		List<Column> columns = new ArrayList<Column>(3);
		Column column = new Column();
		column.setData("name");
		column.setName("name");
		column.setSearchable(Boolean.TRUE);
		column.setOrderable(Boolean.TRUE);
		column.setSearch(new Search("", Boolean.FALSE));
		columns.add(column);

		column = new Column();
		column.setData("englishName");
		column.setName("englishName");
		column.setSearch(new Search("", Boolean.FALSE));
		column.setSearchable(Boolean.TRUE);
		column.setOrderable(Boolean.TRUE);
		columns.add(column);

		column = new Column();
		column.setData("altName");
		column.setName("altName");
		column.setSearchable(Boolean.TRUE);
		column.setOrderable(Boolean.FALSE);
		columns.add(column);

		input.setColumns(columns);
		input.setLength(request.getLimit() != null ? request.getLimit() : -1);
		if (request.getPage() != null && request.getLimit()!=null) {
			input.setStart(request.getPage() * request.getLimit());
		}
		if (request.getSearch() != null) {
			if (request.getSearch().getValue() != null && !request.getSearch().getValue().isEmpty()) {
				if (request.getSearch().getExact() != null && request.getSearch().getExact()) {
					specification = (root, query, cb) -> cb.equal(root.get("name"), request.getSearch().getValue().trim().toUpperCase());
				} else {
					input.getSearch().setValue(request.getSearch().getValue());
					input.getSearch().setRegex(Boolean.FALSE);
				}
			}
		}
		if (request.getFilter() != null) {
			if (!request.getFilter().getBooks().isEmpty()) {
				specification = SpecificationUtil.getAndSpecification(specification, (root, query, cb) -> {
					Join<Book, Spell> join = root.join("book", JoinType.INNER);
					return join.get("source").in(request.getFilter().getBooks());
				});
			}
			if (!request.getFilter().getCategories().isEmpty()) {
				specification = SpecificationUtil.getAndSpecification(specification,
						(root, query, cb) -> root.get("type").in(
								request.getFilter().getCategories().stream().map(TreasureType::valueOf).collect(Collectors.toList()))
						);
			}
		}
		if (request.getOrders() !=null && !request.getOrders().isEmpty()) {
			specification = SpecificationUtil.getAndSpecification(specification, (root, query, cb) -> {
				List<Order> orders = request.getOrders().stream()
						.map(
							order -> "asc".equals(order.getDirection()) ? cb.asc(root.get(order.getField())) : cb.desc(root.get(order.getField()))
						)
						.collect(Collectors.toList());
				query.orderBy(orders);
				return cb.and();
			});
		}
		return treasuryRepository.findAll(input, specification, specification, ItemApi::new).getData();
	}

	@PostMapping("/api/v1/filters/treasures")
	public FilterApi getWeaponsFilter() {
		FilterApi filters = new FilterApi();
		List<FilterApi> sources = new ArrayList<>();
		FilterApi spellMainFilter = new FilterApi("main");
		spellMainFilter.setValues(
				treasuryRepository.findBook(TypeBook.OFFICAL).stream()
				.map(book -> new FilterValueApi(book.getSource(), book.getSource(),	Boolean.TRUE, book.getName()))
				.collect(Collectors.toList()));
		sources.add(spellMainFilter);
		
		FilterApi settingFilter = new FilterApi("Сеттинги", "settings");
		settingFilter.setValues(
				treasuryRepository.findBook(TypeBook.SETTING).stream()
				.map(book -> new FilterValueApi(book.getSource(), book.getSource(),	Boolean.TRUE, book.getName()))
				.collect(Collectors.toList()));
		sources.add(settingFilter);
		
		FilterApi adventureFilter = new FilterApi("Приключения", "adventures");
		adventureFilter.setValues(
				treasuryRepository.findBook(TypeBook.MODULE).stream()
				.map(book -> new FilterValueApi(book.getSource(), book.getSource(),	Boolean.TRUE, book.getName()))
				.collect(Collectors.toList()));
		sources.add(adventureFilter);
		
		FilterApi homebrewFilter = new FilterApi("Homebrew", "homebrew");
		homebrewFilter.setValues(
				treasuryRepository.findBook(TypeBook.CUSTOM).stream()
				.map(book -> new FilterValueApi(book.getSource(), book.getSource(),	Boolean.TRUE, book.getName()))
				.collect(Collectors.toList()));
		sources.add(homebrewFilter);
		filters.setSources(sources);
		
		List<FilterApi> otherFilters = new ArrayList<>();
		
		FilterApi damageTypeFilter = new FilterApi("Тип", "category");
		damageTypeFilter.setValues(
				Arrays.stream(TreasureType.values())
				 .map(value -> new FilterValueApi(value.getName(), value.name()))
				 .collect(Collectors.toList()));
		otherFilters.add(damageTypeFilter);

		filters.setOther(otherFilters);
		return filters;
	}
}