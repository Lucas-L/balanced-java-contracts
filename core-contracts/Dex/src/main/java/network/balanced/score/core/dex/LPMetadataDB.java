/*
 * Copyright (c) 2022-2022 Balanced.network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package network.balanced.score.core.dex;

import network.balanced.score.lib.utils.EnumerableSetDB;
import score.Address;

import java.math.BigInteger;

public class LPMetadataDB {
    private static final String LP_METADATA_PREFIX = "lp";

    public EnumerableSetDB<Address> get(BigInteger id) {
        return new EnumerableSetDB<>(LP_METADATA_PREFIX + id.toString(10), Address.class);
    }

}
