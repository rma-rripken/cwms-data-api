/*
 * MIT License
 *
 * Copyright (c) 2023 Hydrologic Engineering Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cwms.cda.data.dto;

public class AssignedLocation extends CwmsDTOBase
{
	private String locationId;
	private String officeId;
	private String aliasId;
	private Number attribute;

	private String refLocationId;

	public AssignedLocation()
	{

	}

	public AssignedLocation(String locationId, String office, String aliasId,
							Number attribute, String refLocationId)
	{
		this.locationId = locationId;
		this.officeId = office;
		this.aliasId = aliasId;
		this.attribute = attribute;
		this.refLocationId = refLocationId;
	}

	public String getLocationId()
	{
		return locationId;
	}

	public String getOfficeId()
	{
		return officeId;
	}

	public String getAliasId()
	{
		return aliasId;
	}

	public Number getAttribute()
	{
		return attribute;
	}

	public String getRefLocationId()
	{
		return refLocationId;
	}
}
