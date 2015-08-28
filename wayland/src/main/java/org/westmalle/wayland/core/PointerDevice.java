//Copyright 2015 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.westmalle.wayland.core;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;
import org.freedesktop.wayland.server.Client;
import org.freedesktop.wayland.server.DestroyListener;
import org.freedesktop.wayland.server.Display;
import org.freedesktop.wayland.server.WlBufferResource;
import org.freedesktop.wayland.server.WlPointerResource;
import org.freedesktop.wayland.server.WlSurfaceRequests;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.freedesktop.wayland.shared.WlPointerButtonState;
import org.freedesktop.wayland.util.Fixed;
import org.westmalle.wayland.core.events.Button;
import org.westmalle.wayland.core.events.Motion;
import org.westmalle.wayland.core.events.PointerFocusChanged;
import org.westmalle.wayland.core.events.PointerFocusGained;
import org.westmalle.wayland.core.events.PointerFocusLost;
import org.westmalle.wayland.core.events.PointerGrabChanged;
import org.westmalle.wayland.protocol.WlSurface;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoFactory(className = "PointerDeviceFactory")
public class PointerDevice implements Role {

    @Nonnull
    private final Bus                            bus            = new Bus(ThreadEnforcer.ANY);
    @Nonnull
    private final Set<Integer>                   pressedButtons = new HashSet<>();
    @Nonnull
    private final Map<WlPointerResource, Cursor> cursors        = new HashMap<>();
    @Nonnull
    private final CursorFactory  cursorFactory;
    @Nonnull
    private final InfiniteRegion infiniteRegion;
    @Nonnull
    private final NullRegion     nullRegion;
    @Nonnull
    private final JobExecutor    jobExecutor;
    @Nonnull
    private final Compositor     compositor;
    @Nonnull
    private final Display        display;

    @Nonnull
    private Point            position     = Point.ZERO;
    @Nonnull
    private Optional<Cursor> activeCursor = Optional.empty();

    @Nonnull
    private Optional<DestroyListener>   grabDestroyListener = Optional.empty();
    @Nonnull
    private Optional<WlSurfaceResource> grab                = Optional.empty();

    @Nonnull
    private Optional<WlSurfaceResource> focus                = Optional.empty();
    @Nonnull
    private Optional<DestroyListener>   focusDestroyListener = Optional.empty();

    private int buttonPressSerial;
    private int buttonReleaseSerial;
    private int enterSerial;
    private int leaveSerial;

    @Nonnegative
    private int buttonsPressed;

    PointerDevice(@Provided @Nonnull final Display display,
                  @Provided @Nonnull final InfiniteRegion infiniteRegion,
                  @Provided @Nonnull final NullRegion nullRegion,
                  @Provided @Nonnull final CursorFactory cursorFactory,
                  @Provided @Nonnull final JobExecutor jobExecutor,
                  @Nonnull final Compositor compositor) {
        this.display = display;
        this.infiniteRegion = infiniteRegion;
        this.nullRegion = nullRegion;
        this.cursorFactory = cursorFactory;
        this.jobExecutor = jobExecutor;
        this.compositor = compositor;
    }

    public void motion(@Nonnull final Set<WlPointerResource> wlPointerResources,
                       final int x,
                       final int y) {
        final int time = this.compositor.getTime();
        this.position = Point.create(x,
                                     y);

        if (!getGrab().isPresent()) {
            calculateFocus(wlPointerResources);
        }

        getFocus().ifPresent(wlSurfaceResource ->
                                     reportMotion(wlPointerResources,
                                                  time,
                                                  wlSurfaceResource));
        this.bus.post(Motion.create(time,
                                    getPosition()));
    }

    public void calculateFocus(@Nonnull final Set<WlPointerResource> wlPointerResources) {
        final Optional<WlSurfaceResource> oldFocus = getFocus();
        final Optional<WlSurfaceResource> newFocus = over();

        if (!oldFocus.equals(newFocus)) {
            updateFocus(wlPointerResources,
                        oldFocus,
                        newFocus);
        }
    }

    private void reportMotion(final Set<WlPointerResource> wlPointerResources,
                              final int time,
                              final WlSurfaceResource wlSurfaceResource) {

        final Point pointerPosition = getPosition();

        match(wlPointerResources,
              wlSurfaceResource).forEach(wlPointerResource -> {
            final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
            final Point relativePoint = wlSurface.getSurface()
                                                 .local(pointerPosition);
            wlPointerResource.motion(time,
                                     Fixed.create(relativePoint.getX()),
                                     Fixed.create(relativePoint.getY()));
        });

        this.activeCursor.ifPresent(cursor -> cursor.updatePosition(pointerPosition));
    }

    @Nonnull
    public Optional<WlSurfaceResource> over() {
        final Iterator<WlSurfaceResource> surfaceIterator = this.compositor.getSurfacesStack()
                                                                           .descendingIterator();
        Optional<WlSurfaceResource> pointerOver = Optional.empty();
        while (surfaceIterator.hasNext()) {
            final WlSurfaceResource surfaceResource = surfaceIterator.next();
            final WlSurfaceRequests implementation = surfaceResource.getImplementation();
            final Surface surface = ((WlSurface) implementation).getSurface();

            //surface can be invisible (null buffer), in which case we should ignore it.
            if (!surface.getState()
                        .getBuffer()
                        .isPresent()) {
                continue;
            }

            final Optional<Region> inputRegion = surface.getState()
                                                        .getInputRegion();
            final Region region = inputRegion.orElseGet(() -> this.infiniteRegion);

            if (region.contains(surface.getSize(),
                                surface.local(getPosition()))) {
                pointerOver = Optional.of(surfaceResource);
                break;
            }
        }

        return pointerOver;
    }

    private void reportLeave(final Set<WlPointerResource> wlPointerResources,
                             final WlSurfaceResource wlSurfaceResource) {
        wlPointerResources.forEach(wlPointerResource -> wlPointerResource.leave(nextLeaveSerial(),
                                                                                wlSurfaceResource));
    }

    private void reportEnter(final Set<WlPointerResource> wlPointerResources,
                             final WlSurfaceResource wlSurfaceResource) {
        wlPointerResources.forEach(wlPointerResource -> {
            final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
            final Point relativePoint = wlSurface.getSurface()
                                                 .local(getPosition());
            wlPointerResource.enter(nextEnterSerial(),
                                    wlSurfaceResource,
                                    Fixed.create(relativePoint.getX()),
                                    Fixed.create(relativePoint.getY()));
        });
    }

    public int nextLeaveSerial() {
        this.leaveSerial = this.display.nextSerial();
        return getLeaveSerial();
    }

    public int nextEnterSerial() {
        this.enterSerial = this.display.nextSerial();
        return getEnterSerial();
    }

    public int getLeaveSerial() {
        return this.leaveSerial;
    }

    public void button(@Nonnull final Set<WlPointerResource> wlPointerResources,
                       @Nonnegative final int button,
                       @Nonnull final WlPointerButtonState wlPointerButtonState) {
        final int time = this.compositor.getTime();
        if (wlPointerButtonState == WlPointerButtonState.PRESSED) {
            this.pressedButtons.add(button);
        }
        else {
            this.pressedButtons.remove(button);
        }
        doButton(wlPointerResources,
                 time,
                 button,
                 wlPointerButtonState);
        this.bus.post(Button.create(time,
                                    button,
                                    wlPointerButtonState));
    }

    private void doButton(final Set<WlPointerResource> wlPointerResources,
                          final int time,
                          final int button,
                          final WlPointerButtonState wlPointerButtonState) {

        if (wlPointerButtonState == WlPointerButtonState.PRESSED) {
            this.buttonsPressed++;
        }
        else if (this.buttonsPressed > 0) {
            //make sure we only decrement if we had a least one increment.
            //Is such a thing even possible? Yes it is. (Aliens pressing a button before starting compositor)
            this.buttonsPressed--;
        }

        if (this.buttonsPressed == 0) {
            ungrab(wlPointerResources);
        }
        else if (!getGrab().isPresent() &&
                 this.focus.isPresent()) {
            //no grab, but we do have a focus and a pressed button. Focused surface becomes grab.
            grab(wlPointerResources);
        }

        getGrab().ifPresent(wlSurfaceResource ->
                                    reportButton(wlPointerResources,
                                                 wlSurfaceResource,
                                                 time,
                                                 button,
                                                 wlPointerButtonState));
    }

    @Nonnull
    public Optional<WlSurfaceResource> getGrab() {
        return this.grab;
    }

    private void reportButton(final Set<WlPointerResource> wlPointerResources,
                              final WlSurfaceResource wlSurfaceResource,
                              final int time,
                              final int button,
                              final WlPointerButtonState wlPointerButtonState) {
        match(wlPointerResources,
              wlSurfaceResource).forEach(wlPointerResource ->
                                                 wlPointerResource.button(wlPointerButtonState == WlPointerButtonState.PRESSED ?
                                                                          nextButtonPressSerial() : nextButtonReleaseSerial(),
                                                                          time,
                                                                          button,
                                                                          wlPointerButtonState.getValue()));
    }

    private void ungrab(final Set<WlPointerResource> wlPointerResources) {
        //grab will be updated, don't listen for previous grab surface destruction.
        getGrab().ifPresent(wlSurfaceResource -> {
            wlSurfaceResource.unregister(this.grabDestroyListener.get());
            final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
            final Surface surface = wlSurface.getSurface();
            final Set<WlPointerResource> ungrabs = filter(wlPointerResources,
                                                          wlSurfaceResource.getClient());
            surface.getPointerGrabs()
                   .removeAll(ungrabs);
            //surface.post(PointerGrabLost.create(ungrabs));
        });
        this.grabDestroyListener = Optional.empty();
        this.grab = Optional.empty();
        this.bus.post(PointerGrabChanged.create(getGrab()));
    }

    private void grab(final Set<WlPointerResource> wlPointerResources) {
        this.grab = getFocus();
        this.grabDestroyListener = Optional.of(() -> ungrab(wlPointerResources));

        final WlSurfaceResource wlSurfaceResource = getGrab().get();
        //if the surface having the grab is destroyed, we clear the grab
        wlSurfaceResource.register(this.grabDestroyListener.get());
        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        final Surface   surface   = wlSurface.getSurface();
        final Set<WlPointerResource> grabs = filter(wlPointerResources,
                                                    wlSurfaceResource.getClient());
        surface.getPointerGrabs()
               .addAll(grabs);
        //surface.post(PointerGrabGained.create(grabs));

        this.bus.post(PointerGrabChanged.create(getGrab()));
    }

    private Set<WlPointerResource> filter(final Set<WlPointerResource> wlPointerResources,
                                          final Client client) {
        //filter out pointer resources that do not belong to the given client.
        return wlPointerResources.stream()
                                 .filter(wlPointerResource -> wlPointerResource.getClient()
                                                                               .equals(client))
                                 .collect(Collectors.toSet());
    }

    private Set<WlPointerResource> match(final Set<WlPointerResource> wlPointerResources,
                                         final WlSurfaceResource wlSurfaceResource) {
        //find keyboard resources that match this keyboard device
        final WlSurface              wlSurface        = (WlSurface) wlSurfaceResource.getImplementation();
        final Surface                surface          = wlSurface.getSurface();
        final Set<WlPointerResource> wlPointerFocuses = new HashSet<>(surface.getPointerFocuses());
        wlPointerFocuses.retainAll(wlPointerResources);

        return wlPointerFocuses;
    }

    public int nextButtonPressSerial() {
        this.buttonPressSerial = this.display.nextSerial();
        return getButtonPressSerial();
    }

    public int nextButtonReleaseSerial() {
        this.buttonReleaseSerial = this.display.nextSerial();
        return getButtonReleaseSerial();
    }

    @Nonnull
    public Optional<WlSurfaceResource> getFocus() {
        return this.focus;
    }

    public int getButtonPressSerial() {
        return this.buttonPressSerial;
    }

    public int getButtonReleaseSerial() {
        return this.buttonReleaseSerial;
    }

    private void updateFocus(final Set<WlPointerResource> wlPointerResources,
                             final Optional<WlSurfaceResource> oldFocus,
                             final Optional<WlSurfaceResource> newFocus) {

        oldFocus.ifPresent(oldFocusResource -> {
            final WlSurface wlSurface = (WlSurface) oldFocusResource.getImplementation();
            final Surface surface = wlSurface.getSurface();

            final Set<WlPointerResource> clientPointerResources = filter(wlPointerResources,
                                                                         oldFocusResource.getClient());
            surface.getPointerFocuses()
                   .removeAll(clientPointerResources);
            surface.post(PointerFocusLost.create(clientPointerResources));

            //remove old focus destroy listener
            oldFocusResource.unregister(this.focusDestroyListener.get());
            //notify client of focus lost
            reportLeave(clientPointerResources,
                        oldFocusResource);
        });
        //clear ref to old destroy listener
        this.focusDestroyListener = Optional.empty();

        newFocus.ifPresent(newFocusResource -> {
            final WlSurface wlSurface = (WlSurface) newFocusResource.getImplementation();
            final Surface surface = wlSurface.getSurface();

            //find pointer resource of new focus
            final Set<WlPointerResource> clientPointerResources = filter(wlPointerResources,
                                                                         newFocusResource.getClient());
            surface.getPointerFocuses()
                   .addAll(clientPointerResources);
            surface.post(PointerFocusGained.create(clientPointerResources));

            //if focus resource is destroyed, trigger schedule focus update. This guarantees that
            //the compositor removes and updates the list of active surfaces first.
            final DestroyListener destroyListener = () -> this.jobExecutor.submit(() -> calculateFocus(wlPointerResources));
            this.focusDestroyListener = Optional.of(destroyListener);
            //add destroy listener
            newFocusResource.register(destroyListener);
            //notify client of new focus
            reportEnter(match(wlPointerResources,
                              newFocusResource),
                        newFocusResource);
        });

        //update focus to new focus
        this.focus = newFocus;
        //notify listeners focus has changed
        this.bus.post(PointerFocusChanged.create());
    }

    public boolean isButtonPressed(@Nonnegative final int button) {
        return this.pressedButtons.contains(button);
    }

    /**
     * Listen for motion as soon as given surface is grabbed.
     * <p/>
     * If another surface already has the grab, the listener
     * is never registered.
     *
     * @param wlSurfaceResource Surface that is grabbed.
     * @param buttonPressSerial Serial that triggered the grab.
     * @param pointerGrabMotion Motion listener.
     *
     * @return true if the listener was registered, false if not.
     */
    public boolean grabMotion(@Nonnull final WlSurfaceResource wlSurfaceResource,
                              final int buttonPressSerial,
                              @Nonnull final PointerGrabMotion pointerGrabMotion) {
        if (!getGrab().isPresent() ||
            !getGrab().get()
                      .equals(wlSurfaceResource) ||
            getButtonPressSerial() != buttonPressSerial) {
            //preconditions not met
            return false;
        }

        final DestroyListener motionListener = new DestroyListener() {
            @Subscribe
            public void handle(final Motion motion) {
                if (getGrab().isPresent() &&
                    getGrab().get()
                             .equals(wlSurfaceResource)) {
                    //there is pointer motion
                    pointerGrabMotion.motion(motion);
                }
                else {
                    //another surface has the grab, stop listening for pointer motion.
                    PointerDevice.this.unregister(this);
                    //stop listening for destroy event
                    wlSurfaceResource.unregister(this);
                }
            }

            @Override
            public void handle() {
                //the surface was destroyed, stop listening for pointer motion.
                PointerDevice.this.unregister(this);
            }
        };
        //listen for pointer motion
        register(motionListener);
        //listen for surface destruction
        wlSurfaceResource.register(motionListener);

        //TODO should we listen for pointer resource destruction and unregister the listener?

        return true;
    }

    public void unregister(@Nonnull final Object listener) {
        this.bus.unregister(listener);
    }

    public void register(@Nonnull final Object listener) {
        this.bus.register(listener);
    }

    public void removeCursor(@Nonnull final WlPointerResource wlPointerResource,
                             final int serial) {
        if (serial != getEnterSerial()) {
            return;
        }
        Optional.ofNullable(this.cursors.remove(wlPointerResource))
                .ifPresent(Cursor::hide);
    }

    public int getEnterSerial() {
        return this.enterSerial;
    }

    public void setCursor(@Nonnull final WlPointerResource wlPointerResource,
                          final int serial,
                          @Nonnull final WlSurfaceResource wlSurfaceResource,
                          final int hotspotX,
                          final int hotspotY) {

        if (serial != getEnterSerial()) {
            return;
        }

        Cursor cursor = this.cursors.get(wlPointerResource);
        final Point hotspot = Point.create(hotspotX,
                                           hotspotY);

        if (cursor == null) {
            cursor = this.cursorFactory.create(wlSurfaceResource,
                                               hotspot);
            this.cursors.put(wlPointerResource,
                             cursor);
            wlPointerResource.register(() -> Optional.ofNullable(this.cursors.remove(wlPointerResource))
                                                     .ifPresent(Cursor::hide));
        }
        else {
            cursor.setWlSurfaceResource(wlSurfaceResource);
            cursor.setHotspot(hotspot);
        }

        cursor.show();
        updateActiveCursor(wlPointerResource);

        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        final Surface   surface   = wlSurface.getSurface();
        surface.setState(updateCursorSurfaceState(wlSurfaceResource,
                                                  surface.getState()));
    }

    private void updateActiveCursor(final WlPointerResource wlPointerResource) {

        final Cursor           cursor    = this.cursors.get(wlPointerResource);
        final Optional<Cursor> oldCursor = this.activeCursor;
        this.activeCursor = Optional.ofNullable(cursor);

        this.activeCursor.ifPresent(clientCursor -> clientCursor.updatePosition(getPosition()));

        if (!oldCursor.equals(this.activeCursor)) {
            oldCursor.ifPresent(Cursor::hide);
        }
    }

    private SurfaceState updateCursorSurfaceState(final WlSurfaceResource wlSurfaceResource,
                                                  final SurfaceState surfaceState) {
        final SurfaceState.Builder surfaceStateBuilder = surfaceState.toBuilder();

        surfaceStateBuilder.inputRegion(Optional.of(this.nullRegion));
        surfaceStateBuilder.buffer(Optional.<WlBufferResource>empty());

        this.activeCursor.ifPresent(clientCursor -> {
            if (clientCursor.getWlSurfaceResource()
                            .equals(wlSurfaceResource) &&
                !clientCursor.isHidden()) {
                //set back the buffer we cleared.
                surfaceStateBuilder.buffer(surfaceState.getBuffer());
                //move visible cursor to top of surface stack
                this.compositor.getSurfacesStack()
                               .remove(wlSurfaceResource);
                this.compositor.getSurfacesStack()
                               .addLast(wlSurfaceResource);
            }
        });

        return surfaceStateBuilder.build();
    }

    @Nonnull
    public Point getPosition() {
        return this.position;
    }

    @Override
    public void beforeCommit(@Nonnull final WlSurfaceResource wlSurfaceResource) {
        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        final Surface   surface   = wlSurface.getSurface();

        surface.setPendingState(updateCursorSurfaceState(wlSurfaceResource,
                                                         surface.getPendingState()));
    }

    @Override
    public void afterDestroy(@Nonnull final WlSurfaceResource wlSurfaceResource) {
        this.cursors.values()
                    .removeIf(cursor -> {
                        if (cursor.getWlSurfaceResource()
                                  .equals(wlSurfaceResource)) {
                            cursor.hide();
                            return true;
                        }
                        else {
                            return false;
                        }
                    });
    }
}