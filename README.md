# Kayji-LPCC — Folia patch workspace cho LPC-Pro

Không gian làm việc để **patch plugin LPC-Pro (bản 2.1.5) chạy trên Folia**, quanh công cụ chính **`FoliaJarPatcher`** viết bằng **ASM**.

> **Tác giả:** Kayji_Tizi

## Thành phần

| Thành phần | Mô tả |
| --- | --- |
| `patcher/` | Dự án Maven `local.lpcpro:folia-patcher:1.0.0` — công cụ patch jar |
| `local.lpcpro.FoliaJarPatcher` | CLI: `FoliaJarPatcher <input.jar> <output.jar>` |
| `com.wikmor.lpcpro.folia.*` | Lớp compat Folia (scheduler, audience, display, entity, server, listener) |
| `cfr-0.152.jar` | Decompiler CFR (GPL) dùng để đọc jar |

## `FoliaJarPatcher` làm gì?

1. Đọc từng entry của jar input bằng `JarInputStream`.
2. **`plugin.yml`**: sửa để khai báo tương thích Folia.
3. **Mọi class `com/wikmor/lpcpro/**`**: dùng ASM (`ClassReader`/`ClassWriter` + `ClassVisitor`) **chèn lời gọi tới helper `com/wikmor/lpcpro/folia/FoliaScheduler`** thay cho scheduler Bukkit thường.
4. Ghi toàn bộ ra jar output mới — **không sửa jar gốc**.

```bash
cd patcher
mvn package
java -cp target/folia-patcher-1.0.0.jar:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
     local.lpcpro.FoliaJarPatcher input.jar output.jar
```

## Bảng lệnh

Công cụ dòng lệnh, **không có lệnh trong game**:

| Lệnh | Quyền | Mô tả |
| --- | --- | --- |
| `FoliaJarPatcher <input.jar> <output.jar>` | — | Patch jar LPC-Pro sang bản tương thích Folia |

## Cài đặt / Build

```bash
cd patcher
mvn package
```

Dependency: `paper-api` 1.20.4 (provided), `asm` + `asm-commons` 9.7.1.

## Cấu trúc dự án

```
├── patcher/
│   ├── pom.xml                                  local.lpcpro:folia-patcher:1.0.0
│   └── src/main/java
│       ├── local/lpcpro/FoliaJarPatcher.java    CLI patch jar bằng ASM
│       └── com/wikmor/lpcpro/folia/*.java       Lớp compat Folia
│           + bubble/, listener/                 Compat bong bóng chat & listener
├── cfr-0.152.jar                                Decompiler (GPL)
└── .github/                                     Nhật ký công cụ nâng cấp Java
```

### Không có trong repo

Các mục sau **không được commit** (xem `.gitignore`) — chúng là bản phân phối nguyên vẹn của plugin bên thứ ba, cần có local nếu bạn cần:

- `LPC-Pro-2.1.5.jar`, `LPC-Pro-2.1.5-Folia.jar`
- `decompiled/`, `jar_extract/`, `folia_extract/`, `folia_full_extract/`

## Giấy phép

[GNU General Public License v3.0](LICENSE) — áp dụng cho code của dự án này.
