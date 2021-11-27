package io.github.coolcrabs.brachyura.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.util.PathUtil;
import net.fabricmc.mappingio.tree.MappingTree;

class J8FabricProjectTest {
    FabricProject fabricProject = new FabricProject() {
        @Override
        public String getMcVersion() {
            return "1.16.5";
        }

        @Override
        public MappingTree createMappings() {
            MappingTree tree = Yarn.ofMaven(FabricMaven.URL, FabricMaven.yarn("1.16.5+build.10")).tree;
            return tree;
        }

        @Override
        public FabricLoader getLoader() {
            return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.12.5"));
        }

        @Override
        public Path getProjectDir() {
            Path result = PathUtil.CWD.getParent().resolve("testmod");
            assertTrue(Files.isDirectory(result)); 
            return result;
        }

        @Override
        public void getModDependencies(ModDependencyCollector d) {
            d.addMaven(FabricMaven.URL, new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-resource-loader-v0", "0.4.8+3cc0f0907d"), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE, ModDependencyFlag.JIJ);
            d.addMaven(FabricMaven.URL, new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-game-rule-api-v1", "1.0.7+3cc0f0907d"), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE, ModDependencyFlag.JIJ);
        };

        @Override
        public BrachyuraDecompiler decompiler() {
            return null;
        };
    };

    @Test
    void compile() {
        try {
            assertTrue(fabricProject.build());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void ide() {
        long a = System.currentTimeMillis();
        //Todo better api for this?
        fabricProject.getTasks(p -> {
            try {
                if (p.name.equals("vscode")) p.doTask(new String[]{});
                if (p.name.equals("netbeans")) p.doTask(new String[]{});
                if (p.name.equals("idea")) p.doTask(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        });
        long b = System.currentTimeMillis();
        System.out.println(b - a);
    }
}