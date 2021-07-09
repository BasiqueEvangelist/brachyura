package io.github.coolcrabs.brachyura.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.util.PathUtil;
import net.fabricmc.mappingio.tree.MappingTree;

class FabricProjectTest {
    FabricProject fabricProject = new FabricProject() {
        @Override
        String getModId() {
            return "brachyuratestmod";
        }

        @Override
        String getVersion() {
            return "1.0.0";
        }

        @Override
        String getMcVersion() {
            return "1.16.5";
        }

        @Override
        MappingTree getMappings() {
            MappingTree tree = Yarn.ofMaven(FabricMaven.URL, FabricMaven.yarn("1.16.5+build.10")).tree;
            return tree;
        }

        @Override
        FabricLoader getLoader() {
            return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.11.6"));
        }

        @Override
        public Path getProjectDir() {
            Path result = PathUtil.CWD.getParent().resolve("testmod");
            assertTrue(Files.isDirectory(result)); 
            return result;
        }
        
    };

    @Test
    void testProject() {
        assertTrue(Files.isRegularFile(fabricProject.getIntermediaryJar().jar));
        assertTrue(Files.isRegularFile(fabricProject.getNamedJar().jar));
        assertTrue(Files.isRegularFile(fabricProject.getDecompiledJar()));
    }
    
    @Test
    void vscode() {
        fabricProject.vscode();
    }

    @Test
    void compile() {
        fabricProject.compile();
    }
}