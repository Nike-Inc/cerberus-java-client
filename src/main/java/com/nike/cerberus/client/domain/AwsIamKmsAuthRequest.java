/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.client.domain;

import java.util.Objects;

/** Represents the IAM principal credentials sent during authentication. */
public class AwsIamKmsAuthRequest {

  private String iamPrincipalArn;

  private String region;

  public String getIamPrincipalArn() {
    return iamPrincipalArn;
  }

  public void setIamPrincipalArn(String iamPrincipalArn) {
    this.iamPrincipalArn = iamPrincipalArn;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsIamKmsAuthRequest that = (AwsIamKmsAuthRequest) o;
    return Objects.equals(iamPrincipalArn, that.iamPrincipalArn)
        && Objects.equals(region, that.region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(iamPrincipalArn, region);
  }
}
