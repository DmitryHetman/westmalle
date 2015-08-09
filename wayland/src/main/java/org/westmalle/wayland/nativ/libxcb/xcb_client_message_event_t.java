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
package org.westmalle.wayland.nativ.libxcb;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class xcb_client_message_event_t extends Structure {

    private static final List<?> FIELD_ORDER = Arrays.asList("response_type",
                                                             "format",
                                                             "sequence",
                                                             "window",
                                                             "type",
                                                             "data");

    public byte                              response_type;
    public byte                              format;
    public short                             sequence;
    public int                               window;
    public int                               type;
    public xcb_client_message_data_t.ByValue data;

    @Override
    protected List<?> getFieldOrder() {
        return FIELD_ORDER;
    }
}