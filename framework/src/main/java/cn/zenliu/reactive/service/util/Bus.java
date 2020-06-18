/*
 *  Copyright (c) 2020.  Zen.Liu .
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *   @Project: reactive-service-framework
 *   @Module: reactive-service-framework
 *   @File: ReactiveEventBus.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-14 21:24:57
 */

package cn.zenliu.reactive.service.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Event bus base on Reactive Stream (Reactor)
 * can be extends or just use {@link Bus#defaultBus}
 *
 * @param <T>
 */
public interface Bus<T extends Bus.Event> {
    /**
     * just some pure interface as a marker type
     */
    interface Event {
    }

    //region Defines
    boolean isRunning();

    <R extends T> void publish(@NotNull final R event);


    Disposable subscribe(
        @NotNull final Consumer<? super T> consumer,
        @Nullable final Predicate<T> predicate,
        @Nullable final Scheduler scheduler
    );

    Flux<T> fetchFlux(
        @Nullable final Predicate<T> predicate,
        @Nullable final Scheduler scheduler
    );

    //endregion


    /**
     * a lazy warp of default bus
     */
    Lazy<Bus<Event>> defaultBus = Lazy.laterComputed(() -> new scope.DefaultBus(() -> {
        final DirectProcessor<Event> pro = DirectProcessor.create();
        return pro;
    }));


    @UtilityClass
    class scope {
        public abstract class DefaultAbstractBus<T extends Bus.Event> implements Bus<T> {
            protected DefaultAbstractBus(final Supplier<FluxProcessor<T, T>> supplier) {
                this.supplier = supplier;
            }

            private Flux<T> onErrorHandler(Throwable t) {
                this.prepare();
                //todo default error Handler
                return flux;
            }

            protected synchronized void prepare() {
                final FluxProcessor<T, T> processor = supplier.get();
                this.sink = processor.sink();
                this.flux = processor.onErrorResume(this::onErrorHandler);
            }

            protected volatile FluxSink<T> sink;
            protected volatile Flux<T> flux;
            protected final Supplier<FluxProcessor<T, T>> supplier;
            protected final List<Disposable> disposables = new ArrayList<>();


            /**
             * check if event bus is running
             *
             * @return bool
             */
            @Override
            public boolean isRunning() {
                return !disposables.isEmpty() && !disposables.stream().allMatch(Disposable::isDisposed);
            }

            @Override
            /**
             * publish some event
             *
             * @param event event to send
             */
            public synchronized <R extends T> void publish(@NotNull final R event) {
                sink.next(event);
            }

            @Override
            public Flux<T> fetchFlux(
                final @Nullable Predicate<T> predicate,
                final @Nullable Scheduler scheduler) {
                if(this.flux==null) this.prepare();
                Flux<T> fx = flux.share();
                if (scheduler != null)
                    fx = fx.subscribeOn(scheduler);
                if (predicate != null) fx = fx.filter(predicate);
                return fx;
            }

            @Override
            public Disposable subscribe(
                @NotNull final Consumer<? super T> consumer,
                @Nullable final Predicate<T> predicate,
                @Nullable final Scheduler scheduler
            ) {
                if(this.flux==null) this.prepare();
                synchronized (disposables) {
                    Flux<T> fx = flux.share();
                    if (scheduler != null)
                        fx = fx.subscribeOn(scheduler);
                    if (predicate != null) fx = fx.filter(predicate);
                    final Disposable disposable = fx.subscribe(consumer);
                    disposables.add(disposable);
                    return disposable;
                }
            }


            /**
             * stop event bus if any of it is running
             */
            public void stop() {
                if (isRunning()) disposables.forEach(Disposable::dispose);
            }
        }

        protected final class DefaultBus extends DefaultAbstractBus<Bus.Event> {

            protected DefaultBus(final Supplier<FluxProcessor<Event, Event>> supplier) {
                super(supplier);
            }
        }

    }
}
