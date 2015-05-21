/*
 * Copyright 2015 Kaazing Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.real_logic.aeron.staf;

import com.ibm.staf.*;

import java.util.List;
import java.util.Map;

public class AeronSTAFRunner
{
    public int stafHelper(final String[] args)
    {

        return 0;
    }

    public static void main(String[] args)
    {
        try
        {
            final STAFHandle handle = new STAFHandle("TestProcess");
            System.out.println("STAF handle: " + handle.getHandle());

            try
            {
                final String machine = "local";
                final String command = "ls";
                final String service = "Process";
                final String request = "START SHELL COMMAND " + STAFUtil.wrapData(command) + " WAIT RETURNSTDOUT STDERRTOSTDOUT";
                final STAFResult result = handle.submit2(machine, service, request);
                if (result.rc != 0)
                {
                    System.out.println("ERROR: STAF " + machine + " " + service + " " + request +
                            " RC: " + result.rc + ", Result: " + result.result);
                    System.exit(1);
                }

                final Map resultMap = (Map) result.resultObj;
                final String processRC = (String) resultMap.get("rc");

                if (!processRC.equals("0"))
                {
                    System.out.println("ERROR: Process RC is not 0.\n" + result.resultContext);
                    System.exit(1);
                }

                final List returnedFileList = (List) resultMap.get("fileList");
                final Map stdoutMap = (Map) returnedFileList.get(0);
                final String stdoutData = (String) stdoutMap.get("data");
                System.out.println("Process Stdout:\n" + stdoutData);
            }
            finally
            {
                handle.unRegister();
            }
        }
        catch (STAFException e)
        {
            e.printStackTrace();
        }
    }
}
