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

import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Event bus base on Reactive Stream (Reactor)
 * can be extends or just use {@link ReactiveBus#defaultBus}
 * @param <T>
 */
public abstract class ReactiveBus<T extends ReactiveBus.Event> {
    /**
     * just some pure interface as a marker type
     */
    public interface Event {
    }

    /**
     * a lazy warp of default bus
     */
    @SuppressWarnings("Convert2MethodRef")
    public static final Lazy<ReactiveBus<Event>> defaultBus = Lazy.laterComputed(() -> DefaultBus.get());

    /**
     * default impl
     */
    private static class DefaultBus extends ReactiveBus<Event> {
        DefaultBus(final FluxSink<Event> sink, final Flux<Event> flux) {
            super(sink, flux);
        }
        static DefaultBus get() {
            final DirectProcessor<Event> processor = DirectProcessor.create();
           return new DefaultBus(processor.sink(), processor);
        }
    }

    //region Defines
    protected ReactiveBus(final FluxSink<T> sink, final Flux<T> flux) {
        this.sink = sink;
        this.flux = flux;
    }

    protected final FluxSink<T> sink;
    protected final Flux<T> flux;
    protected final List<Disposable> disposables = new ArrayList<>();


    /**
     * check if event bus is running
     *
     * @return bool
     */
    public boolean isRunning() {
        return !disposables.isEmpty() && !disposables.stream().allMatch(Disposable::isDisposed);
    }

    /**
     * publish some event
     *
     * @param event event to send
     */
    public <R extends T> void publish(@NotNull final R event) {
        synchronized (this.sink) {
            sink.next(event);
        }
    }

    public Disposable subscribe(
        @NotNull final Consumer<? super T> consumer
    ) {
        synchronized (disposables) {
            final Disposable disposable = flux
                .share()
                .subscribe(consumer);
            disposables.add(disposable);
            return disposable;
        }
    }

    public Disposable subscribe(
        @NotNull final Consumer<? super T> consumer,
        @NotNull final Scheduler scheduler
    ) {
        synchronized (disposables) {
            final Disposable disposable = flux
                .share()
                .subscribeOn(scheduler)
                .subscribe(consumer);
            disposables.add(disposable);
            return disposable;
        }
    }

    public Disposable subscribe(
        @NotNull final Predicate<T> predicate,
        @NotNull final Consumer<? super T> consumer
    ) {
        synchronized (disposables) {
            final Disposable disposable = flux
                .share()
                .filter(predicate)
                .subscribe(consumer);
            disposables.add(disposable);
            return disposable;
        }
    }

    public Disposable subscribe(
        @NotNull final Predicate<T> predicate,
        @NotNull final Consumer<? super T> consumer,
        @NotNull final Scheduler scheduler
    ) {
        synchronized (disposables) {
            final Disposable disposable = flux
                .share()
                .subscribeOn(scheduler)
                .filter(predicate)
                .subscribe(consumer);
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
    //endregion
}
