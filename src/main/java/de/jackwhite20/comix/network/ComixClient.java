/*
 * Copyright (c) 2015 "JackWhite20"
 *
 * This file is part of Comix.
 *
 * Comix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.jackwhite20.comix.network;

import de.jackwhite20.comix.handler.DownstreamHandler;
import de.jackwhite20.comix.handler.UpstreamHandler;

/**
 * Created by JackWhite20 on 16.07.2015.
 */
public class ComixClient {

    private String name;

    private DownstreamHandler downstreamHandler;

    private UpstreamHandler upstreamHandler;

    public ComixClient(String name, DownstreamHandler downstreamHandler, UpstreamHandler upstreamHandler) {
        this.name = name;
        this.downstreamHandler = downstreamHandler;
        this.upstreamHandler = upstreamHandler;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DownstreamHandler getDownstreamHandler() {
        return downstreamHandler;
    }

    public UpstreamHandler getUpstreamHandler() {
        return upstreamHandler;
    }

    public long getUpstreamBytesIn() {
        return upstreamHandler.getUpstreamBytesIn();
    }

    public long getDownstreamBytesOut() {
        return upstreamHandler.getDownstreamBytesOut();
    }

    public long getUpstreamBytesOut() {
        return downstreamHandler.getUpstreamBytesOut();
    }

    public long getDownstreamBytesIn() {
        return downstreamHandler.getDownstreamBytesIn();
    }

}
