// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazonaws.c3r.cli;

import com.amazonaws.c3r.cleanrooms.CleanRoomsDao;
import com.amazonaws.c3r.cleanrooms.CleanRoomsDaoTestUtility;
import com.amazonaws.c3r.config.ClientSettings;
import com.amazonaws.c3r.io.FileFormat;
import com.amazonaws.c3r.utils.FileTestUtility;
import com.amazonaws.c3r.utils.GeneralTestUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import picocli.CommandLine;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EncryptModeArgsTest {
    private static final String INPUT_PATH = "../samples/csv/data_sample_without_quotes.csv";

    private static final String SCHEMA_PATH = "../samples/schema/config_sample.json";

    private EncryptCliConfigTestUtility encArgs;

    private EncryptMode main;

    private CleanRoomsDao mockCleanRoomsDao;

    @BeforeEach
    public void setup() throws IOException {
        final String output = FileTestUtility.createTempFile().toString();
        encArgs = EncryptCliConfigTestUtility.defaultDryRunTestArgs(INPUT_PATH, SCHEMA_PATH);
        encArgs.setOutput(output);
        mockCleanRoomsDao = CleanRoomsDaoTestUtility.generateMockDao();
        when(mockCleanRoomsDao.getCollaborationDataEncryptionMetadata(any()))
                .thenAnswer((Answer<ClientSettings>) invocation -> encArgs.getClientSettings());
        main = new EncryptMode(mockCleanRoomsDao);
    }

    public int runMainWithCliArgs() {
        return new CommandLine(main).execute(encArgs.toArrayWithoutMode());
    }

    @Test
    public void minimumViableArgsTest() {
        assertEquals(0, runMainWithCliArgs());
        assertEquals(SCHEMA_PATH, main.getRequiredArgs().getSchema());
        assertEquals(INPUT_PATH, main.getRequiredArgs().getInput());
        assertEquals(GeneralTestUtility.EXAMPLE_SALT, main.getRequiredArgs().getId());
    }

    @Test
    public void missingRequiredEncryptArgFailsTest() {
        encArgs.setDryRun(false);
        encArgs.setEnableStackTraces(false);
        encArgs.setOverwrite(false);
        encArgs.setOutput(null);
        final var origArgs = encArgs.getCliArgsWithoutMode();
        for (int i = 0; i < origArgs.size(); i++) {
            final List<String> args = new ArrayList<>(origArgs);
            final String arg = origArgs.get(i);
            args.remove(arg);
            assertNotEquals(0, new CommandLine(main).execute(args.toArray(String[]::new)));
        }
    }

    @Test
    public void validateInputBlankTest() {
        encArgs.setInput("");
        assertNotEquals(0, runMainWithCliArgs());
    }

    @Test
    public void validateConfigBlankTest() {
        encArgs.setSchema("");
        assertNotEquals(0, runMainWithCliArgs());
    }

    @Test
    public void validateCollaborationIdBlankTest() {
        encArgs.setCollaborationId("");
        assertNotEquals(0, runMainWithCliArgs());
    }

    @Test
    public void validateCollaborationIdInvalidUuidTest() {
        encArgs.setCollaborationId("123456");
        assertNotEquals(0, runMainWithCliArgs());
    }

    @Test
    public void getTargetFileEmptyTest() {
        encArgs.setOutput("");
        assertNotEquals(0, runMainWithCliArgs());
    }

    private void checkBooleans(final Function<Boolean, Boolean> action) {
        assertEquals(true, action.apply(true));
        assertEquals(false, action.apply(false));
    }

    @Test
    public void allowCleartextFlagTest() {
        checkBooleans(b -> {
            encArgs.setAllowCleartext(b);
            runMainWithCliArgs();
            return main.getClientSettings().isAllowCleartext();
        });
    }

    @Test
    public void allowDuplicatesFlagTest() {
        checkBooleans(b -> {
            encArgs.setAllowDuplicates(b);
            runMainWithCliArgs();
            return main.getClientSettings().isAllowDuplicates();
        });
    }

    @Test
    public void allowJoinsOnColumnsWithDifferentNamesFlagTest() {
        checkBooleans(b -> {
            encArgs.setAllowJoinsOnColumnsWithDifferentNames(b);
            runMainWithCliArgs();
            return main.getClientSettings().isAllowJoinsOnColumnsWithDifferentNames();
        });

    }

    @Test
    public void preserveNullsFlagTest() {
        checkBooleans(b -> {
            encArgs.setPreserveNulls(b);
            runMainWithCliArgs();
            return main.getClientSettings().isPreserveNulls();
        });
    }

    @Test
    public void inputFileFormatTest() throws IOException {
        final String input = FileTestUtility.createTempFile("input", ".unknown").toString();

        encArgs.setInput(input);
        assertNotEquals(0, runMainWithCliArgs());
        encArgs.setFileFormat(FileFormat.CSV);
        assertEquals(0, runMainWithCliArgs());
    }

    @Test
    public void noProfileOrRegionFlagsTest() {
        main = new EncryptMode(mockCleanRoomsDao);
        new CommandLine(main).execute(encArgs.toArrayWithoutMode());
        assertNull(main.getOptionalArgs().getProfile());
        assertNull(mockCleanRoomsDao.getRegion());
    }

    @Test
    public void profileFlagTest() throws IOException {
        // Ensure that passing a value via the --profile flag is given to the CleanRoomsDao builder's `profile(..)` method.
        final String myProfileName = "my-profile-name";
        assertNotEquals(myProfileName, mockCleanRoomsDao.toString());

        when(mockCleanRoomsDao.withRegion(any())).thenThrow(new RuntimeException("test failure - region should have have been set"));
        encArgs.setProfile(myProfileName);
        main = new EncryptMode(mockCleanRoomsDao);
        new CommandLine(main).execute(encArgs.toArrayWithoutMode());
        assertEquals(myProfileName, main.getOptionalArgs().getProfile());
        assertEquals(myProfileName, main.getCleanRoomsDao().getProfile());
    }

    @Test
    public void regionFlagTest() {
        final String myRegion = "collywobbles";
        encArgs.setRegion(myRegion);
        main = new EncryptMode(mockCleanRoomsDao);
        new CommandLine(main).execute(encArgs.toArrayWithoutMode());
        assertEquals(myRegion, main.getOptionalArgs().getRegion());
        assertEquals(Region.of(myRegion), main.getCleanRoomsDao().getRegion());
    }

}