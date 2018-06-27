/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.heron.healthmgr.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import com.microsoft.dhalion.core.Action;
import com.microsoft.dhalion.core.Diagnosis;
import com.microsoft.dhalion.core.Measurement;
import com.microsoft.dhalion.core.Symptom;
import com.microsoft.dhalion.policy.HealthPolicyImpl;

import org.apache.heron.api.generated.TopologyAPI;
import org.apache.heron.healthmgr.HealthPolicyConfig;
import org.apache.heron.healthmgr.common.PhysicalPlanProvider;

import static org.apache.heron.healthmgr.HealthPolicyConfig.CONF_POLICY_ID;

/**
 * This Policy class
 * 1. has a toggle switch
 * 2. works with runtime config
 */
public class ToggleablePolicy extends HealthPolicyImpl {
  private static final Logger LOG =
      Logger.getLogger(ToggleablePolicy.class.getName());

  protected PhysicalPlanProvider physicalPlanProvider;
  protected String policyId;
  private String policyIdRuntime;
  protected HealthPolicyConfig policyConfig;

  protected PolicyMode policyMode;

  public enum PolicyMode {
    activated,
    deactivated
  }

  @Inject
  public ToggleablePolicy(
      @Named(CONF_POLICY_ID) String policyId,
      HealthPolicyConfig policyConfig,
      PhysicalPlanProvider physicalPlanProvider) {
    this.physicalPlanProvider = physicalPlanProvider;
    this.policyId = policyId;
    this.policyConfig = policyConfig;

    policyMode = policyConfig.getPolicyMode();
    policyIdRuntime = policyId + ":runtime";
  }

  @Override
  public Collection<Measurement> executeSensors() {
    for (TopologyAPI.Config.KeyValue kv
        : physicalPlanProvider.get().getTopology().getTopologyConfig().getKvsList()) {
      LOG.fine("kv " + kv.getKey() + ":" + kv.getValue());
      if (kv.getKey().equals(policyIdRuntime)) {
        PolicyMode val = PolicyMode.valueOf(kv.getValue());
        if (PolicyMode.deactivated.equals(val)
            && policyMode.equals(PolicyMode.activated)) {
          policyMode = PolicyMode.deactivated;
          LOG.info("policy " + policyId + " status changed to " + policyMode);
        } else if (PolicyMode.activated.equals(val)
            && policyMode.equals(PolicyMode.deactivated)) {
          policyMode = PolicyMode.activated;
          LOG.info("policy " + policyId + " status changed to " + policyMode);
        } else {
          LOG.info("policy " + policyId
              + " status does not change " + policyMode + "; input " + val);
        }
        break;
      }
    }

    if (policyMode.equals(PolicyMode.activated)) {
      return super.executeSensors();
    } else {
      return new ArrayList<Measurement>();
    }
  }

  @Override
  public Collection<Symptom> executeDetectors(Collection<Measurement> measurements) {
    if (policyMode.equals(PolicyMode.activated)) {
      return super.executeDetectors(measurements);
    } else {
      return new ArrayList<Symptom>();
    }
  }

  @Override
  public Collection<Diagnosis> executeDiagnosers(Collection<Symptom> symptoms) {
    if (policyMode.equals(PolicyMode.activated)) {
      return super.executeDiagnosers(symptoms);
    } else {
      return new ArrayList<Diagnosis>();
    }
  }

  @Override
  public Collection<Action> executeResolvers(Collection<Diagnosis> diagnosis) {
    if (policyMode.equals(PolicyMode.activated)) {
      return super.executeResolvers(diagnosis);
    } else {
      /*
       * TODO(dhalion):
       * If sub-class could access the `lastExecutionTimestamp`
       * and `oneTimeDelay`, avoid super method invocation.
       */
      return super.executeResolvers(new ArrayList<Diagnosis>());
    }
  }
}
