package org.intellij.plugins.markdown.injection;

import com.intellij.lang.Language;
import com.intellij.lexer.EmbeddedTokenTypesProvider;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public enum LanguageGuesser {
  INSTANCE;

  private final NotNullLazyValue<List<EmbeddedTokenTypesProvider>> embeddedTokenTypeProviders =
    new NotNullLazyValue<List<EmbeddedTokenTypesProvider>>() {
      @NotNull
      @Override
      protected List<EmbeddedTokenTypesProvider> compute() {
        return Arrays.asList(Extensions.getExtensions(EmbeddedTokenTypesProvider.EXTENSION_POINT_NAME));
      }
    };

  private final ClearableLazyValue<List<CodeFenceLanguageProvider>> codeFenceLanguageProviders =
    new ClearableLazyValue<List<CodeFenceLanguageProvider>>() {
      @NotNull
      @Override
      protected List<CodeFenceLanguageProvider> compute() {
        return Arrays.asList(Extensions.getExtensions(CodeFenceLanguageProvider.EP_NAME));
      }
    };

  private final NotNullLazyValue<Map<String, Language>> langIdToLanguage = new NotNullLazyValue<Map<String, Language>>() {
    @NotNull
    @Override
    protected Map<String, Language> compute() {
      final HashMap<String, Language> result = new HashMap<>();
      for (Language language : Language.getRegisteredLanguages()) {
        if (language.getID().isEmpty()) {
          continue;
        }

        result.put(language.getID().toLowerCase(Locale.US), language);
      }

      final Language javascriptLanguage = result.get("javascript");
      if (javascriptLanguage != null) {
        result.put("js", javascriptLanguage);
      }
      return result;
    }
  };

  @NotNull
  public Map<String, Language> getLangToLanguageMap() {
    return Collections.unmodifiableMap(langIdToLanguage.getValue());
  }

  @NotNull
  public List<CodeFenceLanguageProvider> getCodeFenceLanguageProviders() {
    return codeFenceLanguageProviders.getValue();
  }

  @TestOnly
  public void resetCodeFenceLanguageProviders() {
    codeFenceLanguageProviders.drop();
  }

  @Nullable
  public Language guessLanguage(@NotNull String languageName) {
    for (CodeFenceLanguageProvider provider : codeFenceLanguageProviders.getValue()) {
      final Language languageByProvider = provider.getLanguageByInfoString(languageName);
      if (languageByProvider != null) {
        return languageByProvider;
      }
    }

    final Language languageFromMap = langIdToLanguage.getValue().get(languageName.toLowerCase(Locale.US));
    if (languageFromMap != null) {
      return languageFromMap;
    }

    for (EmbeddedTokenTypesProvider provider : embeddedTokenTypeProviders.getValue()) {
      if (provider.getName().equalsIgnoreCase(languageName)) {
        return provider.getElementType().getLanguage();
      }
    }
    return null;
  }
}
