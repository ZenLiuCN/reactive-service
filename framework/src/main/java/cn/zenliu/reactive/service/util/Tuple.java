package cn.zenliu.reactive.service.util;

import lombok.Data;


public interface Tuple {
    static <T, R> Pair<T, R> pairOf(T a, R b) {
        return new Pair<>(a, b);
    }

    static <A, B, C> Triple<A, B, C> tripleOf(A a, B b, C c) {
        return new Triple<>(a, b, c);
    }

    static <A, B, C, D> Quartet<A, B, C, D> quaternionOf(A a, B b, C c, D d) {
        return new Quartet<>(a, b, c, d);
    }

    static <A, B, C, D, E> Quintet<A, B, C, D, E> quaternionOf(A a, B b, C c, D d, E e) {
        return new Quintet<>(a, b, c, d, e);
    }

    @Data
    final class Pair<A, B> implements Tuple {
        final private A first;
        final private B second;
    }

    @Data
    final class Triple<A, B, C> implements Tuple {
        final private A first;
        final private B second;
        final private C third;

    }

    @Data
    final class Quartet<A, B, C, D> implements Tuple {
        final private A first;
        final private B second;
        final private C third;
        final private D fourth;

    }

    @Data
    final class Quintet<A, B, C, D, E> implements Tuple {
        final private A first;
        final private B second;
        final private C third;
        final private D fourth;
        final private E fifth;

    }
}
