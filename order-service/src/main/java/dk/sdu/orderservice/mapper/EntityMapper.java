package dk.sdu.orderservice.mapper;

public interface EntityMapper <E, D> {
    D map(E entity);
}
